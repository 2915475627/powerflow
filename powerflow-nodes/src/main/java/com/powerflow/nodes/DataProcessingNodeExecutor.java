package com.powerflow.nodes;

import com.powerflow.engine.domain.enums.ExecutionStatus;
import com.powerflow.engine.domain.enums.NodeType;
import com.powerflow.engine.domain.model.Context;
import com.powerflow.engine.domain.model.Node;
import com.powerflow.engine.domain.model.NodeResult;
import com.powerflow.engine.domain.port.outbound.NodeExecutorPort;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes DATA_PROCESSING nodes - evaluates SpEL transform expressions.
 */
public class DataProcessingNodeExecutor implements NodeExecutorPort {

    private final ExpressionParser parser = new SpelExpressionParser();
    private static final Pattern MAP_ACCESSOR_PATTERN = Pattern.compile("(#input)\\.([a-zA-Z_][a-zA-Z0-9_]*)");

    @Override
    public NodeType supportedType() {
        return NodeType.DATA_PROCESSING;
    }

    @Override
    public NodeResult execute(Node node, Context context) {
        try {
            String outputKey = (String) node.getConfig().getOrDefault("outputKey", "result");
            String expression = (String) node.getConfig().get("expression");
            String nextNodeId = (String) node.getConfig().get("nextNodeId");

            EvaluationContext evalContext = new StandardEvaluationContext();
            evalContext.setVariable("input", context.toMap());

            String transformedExpr = transformMapAccess(expression);
            Object result = parser.parseExpression(transformedExpr).getValue(evalContext);

            Map<String, Object> output = new HashMap<>();
            output.put(outputKey, result);

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

    private String transformMapAccess(String expression) {
        if (expression == null) return null;
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
