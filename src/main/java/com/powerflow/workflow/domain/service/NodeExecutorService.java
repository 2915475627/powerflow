package com.powerflow.workflow.domain.service;

import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import com.powerflow.workflow.domain.model.enums.NodeType;
import com.powerflow.workflow.domain.port.outbound.NodeExecutorPort;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NodeExecutorService implements NodeExecutorPort {

    private final ExpressionParser parser = new SpelExpressionParser();
    private static final Pattern MAP_ACCESSOR_PATTERN = Pattern.compile("(#input)\\.([a-zA-Z_][a-zA-Z0-9_]*)");

    public NodeExecutorService() {
    }

    @Override
    public NodeResult execute(Node node, Context context) {
        return switch (node.getType()) {
            case DATA_PROCESSING -> executeDataProcessing(node, context);
            case CONDITION -> executeCondition(node, context);
        };
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

    private NodeResult executeDataProcessing(Node node, Context context) {
        try {
            String outputKey = (String) node.getConfig().get("outputKey");
            String expression = (String) node.getConfig().get("expression");
            String nextNodeId = (String) node.getConfig().get("nextNodeId");

            EvaluationContext evalContext = new StandardEvaluationContext();
            evalContext.setVariable("input", context.toMap());

            String transformedExpr = transformMapAccess(expression);
            Object result = parser.parseExpression(transformedExpr).getValue(evalContext);

            Map<String, Object> output = new HashMap<>();
            output.put(outputKey, result);

            if (nextNodeId != null) {
                return NodeResult.builder()
                    .nodeId(node.getId())
                    .status(ExecutionStatus.SUCCESS)
                    .output(output)
                    .nextNodeId(nextNodeId)
                    .build();
            }

            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.SUCCESS)
                .output(output)
                .build();
        } catch (Exception e) {
            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.FAILED)
                .error(e.getMessage())
                .build();
        }
    }

    private NodeResult executeCondition(Node node, Context context) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> conditions = (List<Map<String, String>>) node.getConfig().get("conditions");
            String defaultNextNodeId = (String) node.getConfig().get("defaultNextNodeId");

            EvaluationContext evalContext = new StandardEvaluationContext();
            evalContext.setVariable("input", context.toMap());

            for (Map<String, String> condition : conditions) {
                String expression = condition.get("expression");
                String nextNodeId = condition.get("nextNodeId");

                String transformedExpr = transformMapAccess(expression);
                Boolean result = parser.parseExpression(transformedExpr).getValue(evalContext, Boolean.class);
                if (Boolean.TRUE.equals(result)) {
                    return NodeResult.builder()
                        .nodeId(node.getId())
                        .status(ExecutionStatus.SUCCESS)
                        .nextNodeId(nextNodeId)
                        .output(Map.of())
                        .build();
                }
            }

            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.SUCCESS)
                .nextNodeId(defaultNextNodeId)
                .output(Map.of())
                .build();
        } catch (Exception e) {
            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.FAILED)
                .error(e.getMessage())
                .build();
        }
    }
}