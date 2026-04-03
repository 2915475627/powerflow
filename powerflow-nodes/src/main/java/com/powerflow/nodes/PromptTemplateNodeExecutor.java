package com.powerflow.nodes;

import com.powerflow.engine.domain.enums.ExecutionStatus;
import com.powerflow.engine.domain.enums.NodeType;
import com.powerflow.engine.domain.model.Context;
import com.powerflow.engine.domain.model.Node;
import com.powerflow.engine.domain.model.NodeResult;
import com.powerflow.engine.domain.port.outbound.NodeExecutorPort;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes PROMPT_TEMPLATE nodes - renders prompt templates with variables.
 * Supports {{variable}} syntax for variable interpolation.
 */
public class PromptTemplateNodeExecutor implements NodeExecutorPort {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    @Override
    public NodeType supportedType() {
        return NodeType.PROMPT_TEMPLATE;
    }

    @Override
    public NodeResult execute(Node node, Context context) {
        try {
            String template = (String) node.getConfig().get("template");
            String outputKey = (String) node.getConfig().getOrDefault("outputKey", "prompt");
            @SuppressWarnings("unchecked")
            Map<String, Object> variables = (Map<String, Object>) node.getConfig().get("variables");

            if (template == null || template.isEmpty()) {
                return NodeResult.builder()
                    .nodeId(node.getId())
                    .status(ExecutionStatus.FAILED)
                    .error("Template is empty")
                    .build();
            }

            String rendered = renderTemplate(template, variables, context);

            Map<String, Object> output = new HashMap<>();
            output.put(outputKey, rendered);

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

    /**
     * Render template by replacing {{variable}} with values.
     * Variables are first looked up in the node config's "variables" map,
     * then in the context's data map.
     */
    String renderTemplate(String template, Map<String, Object> variables, Context context) {
        if (template == null) return null;

        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String replacement = lookupVariable(variableName, variables, context);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement != null ? replacement : matcher.group(0)));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String lookupVariable(String name, Map<String, Object> variables, Context context) {
        // First check node config variables
        if (variables != null && variables.containsKey(name)) {
            Object value = variables.get(name);
            return value != null ? value.toString() : null;
        }
        // Then check context
        return context.get(name).map(Object::toString).orElse(null);
    }
}
