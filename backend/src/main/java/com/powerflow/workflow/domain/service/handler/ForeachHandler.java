package com.powerflow.workflow.domain.service.handler;

import com.powerflow.workflow.domain.model.Context;
import com.powerflow.workflow.domain.model.Node;
import com.powerflow.workflow.domain.model.NodeResult;
import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import com.powerflow.workflow.domain.port.outbound.NodeExecutorCallback;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ForeachHandler {

    private NodeExecutorCallback nodeExecutorCallback;
    private final ExpressionParser parser = new SpelExpressionParser();
    private static final Pattern MAP_ACCESSOR_PATTERN = Pattern.compile("(#input)\\.([a-zA-Z_][a-zA-Z0-9_]*)");

    public void setNodeExecutorCallback(NodeExecutorCallback callback) {
        this.nodeExecutorCallback = callback;
    }

    @SuppressWarnings("unchecked")
    public NodeResult execute(Node node, Context context) {
        try {
            String collectionExpr = (String) node.getConfig().get("collection");
            String variableName = (String) node.getConfig().getOrDefault("variableName", "item");
            int maxIterations = (Integer) node.getConfig().getOrDefault("maxIterations", 100);
            List<String> subgraphNodeIds = (List<String>) node.getConfig().get("subgraphNodeIds");
            String nextNodeId = (String) node.getConfig().get("nextNodeId");

            Object collectionObj = evaluate(collectionExpr, context);
            Collection<?> items = collectionObj instanceof Collection ? (Collection<?>) collectionObj : List.of();

            int iterations = 0;
            List<Object> results = new ArrayList<>();

            for (Object item : items) {
                if (iterations >= maxIterations) break;
                results.add(Map.of(
                    "iteration", iterations,
                    variableName, item,
                    "executed", true
                ));
                iterations++;
            }

            Map<String, Object> output = new HashMap<>();
            output.put("results", results);
            output.put("totalIterations", iterations);
            output.put("collectionExpression", collectionExpr);
            output.put("note", "ForeachHandler placeholder - subgraph execution requires workflow context");

            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.SUCCESS)
                .output(output)
                .nextNodeId(nextNodeId)
                .build();

        } catch (Exception e) {
            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.FAILED)
                .error(e.getMessage())
                .build();
        }
    }

    private Object evaluate(String expression, Context context) {
        if (expression == null) return null;
        EvaluationContext evalContext = new StandardEvaluationContext();
        evalContext.setVariable("input", context.toMap());
        String transformed = transformMapAccess(expression);
        return parser.parseExpression(transformed).getValue(evalContext);
    }

    private String transformMapAccess(String expression) {
        Matcher matcher = MAP_ACCESSOR_PATTERN.matcher(expression);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group(1) + "['" + matcher.group(2) + "']";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
