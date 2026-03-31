package com.powerflow.workflow.domain.service.handler;

import com.powerflow.workflow.domain.model.Context;
import com.powerflow.workflow.domain.model.Node;
import com.powerflow.workflow.domain.model.NodeResult;
import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LlmCallHandler {

    private static final Pattern MAP_ACCESSOR_PATTERN = Pattern.compile("(#input)\\.([a-zA-Z_][a-zA-Z0-9_]*)");

    public NodeResult execute(Node node, Context context) {
        try {
            String provider = (String) node.getConfig().getOrDefault("provider", "openai");
            String model = (String) node.getConfig().getOrDefault("model", "gpt-4");
            String prompt = (String) node.getConfig().get("prompt");
            String outputKey = (String) node.getConfig().getOrDefault("outputKey", "llmResponse");

            String resolvedPrompt = resolveExpression(prompt, context);

            Map<String, Object> output = new HashMap<>();
            output.put(outputKey, Map.of(
                "content", "LLM response placeholder - provider: " + provider + ", model: " + model,
                "prompt", resolvedPrompt,
                "provider", provider,
                "model", model
            ));

            String nextNodeId = (String) node.getConfig().get("nextNodeId");

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

    private String resolveExpression(String expression, Context context) {
        if (expression == null) return null;
        org.springframework.expression.ExpressionParser parser =
            new org.springframework.expression.spel.standard.SpelExpressionParser();
        org.springframework.expression.EvaluationContext evalContext =
            new org.springframework.expression.spel.support.StandardEvaluationContext();
        evalContext.setVariable("input", context.toMap());
        String transformed = transformMapAccess(expression);
        Object result = parser.parseExpression(transformed).getValue(evalContext);
        return result != null ? result.toString() : expression;
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
