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

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes BRANCH nodes - evaluates conditions and routes to matching branch.
 */
public class BranchNodeExecutor implements NodeExecutorPort {

    private final ExpressionParser parser = new SpelExpressionParser();
    private static final Pattern MAP_ACCESSOR_PATTERN = Pattern.compile("(#input)\\.([a-zA-Z_][a-zA-Z0-9_]*)");

    @Override
    public NodeType supportedType() {
        return NodeType.BRANCH;
    }

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult execute(Node node, Context context) {
        try {
            List<Map<String, Object>> branches = (List<Map<String, Object>>) node.getConfig().get("branches");
            String defaultNextNodeId = (String) node.getConfig().get("defaultNextNodeId");

            EvaluationContext evalContext = new StandardEvaluationContext();
            evalContext.setVariable("input", context.toMap());

            for (Map<String, Object> branch : branches) {
                String expression = (String) branch.get("expression");
                String nextNodeId = (String) branch.get("nextNodeId");
                String name = (String) branch.getOrDefault("name", "unnamed");

                String transformedExpr = transformMapAccess(expression);
                Boolean result = parser.parseExpression(transformedExpr).getValue(evalContext, Boolean.class);
                if (Boolean.TRUE.equals(result)) {
                    return NodeResult.builder()
                        .nodeId(node.getId())
                        .status(ExecutionStatus.SUCCESS)
                        .nextNodeId(nextNodeId)
                        .output(Map.of("matchedBranch", name))
                        .build();
                }
            }

            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.SUCCESS)
                .nextNodeId(defaultNextNodeId)
                .output(Map.of("matchedBranch", "default"))
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
