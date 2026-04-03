package com.powerflow.nodes;

import com.powerflow.engine.domain.enums.ExecutionStatus;
import com.powerflow.engine.domain.enums.NodeType;
import com.powerflow.engine.domain.model.Context;
import com.powerflow.engine.domain.model.Node;
import com.powerflow.engine.domain.model.NodeResult;
import com.powerflow.engine.domain.model.llm.LlmConfig;
import com.powerflow.engine.domain.model.llm.LlmProvider;
import com.powerflow.engine.domain.model.llm.LlmProviderFactory;
import com.powerflow.engine.domain.port.outbound.NodeExecutorPort;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes LLM_CALL nodes - calls large language models.
 */
public class LlmCallNodeExecutor implements NodeExecutorPort {

    private static final Pattern MAP_ACCESSOR_PATTERN = Pattern.compile("(#input)\\.([a-zA-Z_][a-zA-Z0-9_]*)");

    @Override
    public NodeType supportedType() {
        return NodeType.LLM_CALL;
    }

    @Override
    public NodeResult execute(Node node, Context context) {
        try {
            String providerName = (String) node.getConfig().getOrDefault("provider", "openai");
            String model = (String) node.getConfig().getOrDefault("model", "gpt-4");
            String prompt = (String) node.getConfig().get("prompt");
            String outputKey = (String) node.getConfig().getOrDefault("outputKey", "llmResponse");
            double temperature = getDouble(node.getConfig(), "temperature", 0.7);
            int maxTokens = getInt(node.getConfig(), "maxTokens", 2048);
            String systemPrompt = (String) node.getConfig().get("systemPrompt");
            // Get apiKey from node config, fallback to context (for runtime-provided credentials)
            String apiKey = (String) node.getConfig().getOrDefault("apiKey",
                context.get("apiKey").map(Object::toString).orElse(null));
            String baseUrl = (String) node.getConfig().getOrDefault("baseUrl",
                context.get("baseUrl").map(Object::toString).orElse(null));

            String resolvedPrompt = resolveExpression(prompt, context);

            // Build LLM config
            LlmConfig config = LlmConfig.builder()
                .model(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .systemPrompt(systemPrompt)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

            // Get provider
            LlmProvider provider = LlmProviderFactory.get(providerName);

            // Get conversation history from context if available
            List<LlmProvider.ChatMessage> history = context.getChatHistory();
            String response;
            if (history != null && !history.isEmpty()) {
                response = provider.generateWithHistory(resolvedPrompt, history, config);
            } else {
                response = provider.generate(resolvedPrompt, config);
            }

            Map<String, Object> output = new HashMap<>();
            output.put(outputKey, response);

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
        // If expression doesn't contain SpEL variables, return as-is
        if (!containsSpelVariables(expression)) {
            return expression;
        }
        ExpressionParser parser = new SpelExpressionParser();
        EvaluationContext evalContext = new StandardEvaluationContext();
        evalContext.setVariable("input", context.toMap());
        String transformed = transformMapAccess(expression);
        Object result = parser.parseExpression(transformed).getValue(evalContext);
        return result != null ? result.toString() : expression;
    }

    private boolean containsSpelVariables(String expression) {
        // Check for SpEL variable references (#) or template expressions
        return expression.contains("#");
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

    private double getDouble(Map<String, Object> config, String key, double defaultValue) {
        Object value = config.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int getInt(Map<String, Object> config, String key, int defaultValue) {
        Object value = config.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
