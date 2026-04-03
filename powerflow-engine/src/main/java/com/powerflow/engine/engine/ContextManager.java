package com.powerflow.engine.engine;

import com.powerflow.engine.domain.model.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages execution context - handles input/output mapping between nodes.
 */
public class ContextManager {

    /**
     * Extract node input from context based on inputMapping.
     * If inputMapping is empty, returns all context data.
     */
    public Map<String, Object> extractNodeInput(Map<String, String> inputMapping, Context context) {
        Map<String, Object> result = new HashMap<>();

        if (inputMapping == null || inputMapping.isEmpty()) {
            return new HashMap<>(context.toMap());
        }

        for (Map.Entry<String, String> entry : inputMapping.entrySet()) {
            String contextKey = entry.getValue().replace("input.", "");
            context.get(contextKey).ifPresent(value -> result.put(entry.getKey(), value));
        }
        return result;
    }

    /**
     * Write node output back to context based on outputMapping.
     * If outputMapping is empty, writes all output keys to context with their original keys.
     */
    public void writeNodeOutput(Map<String, String> outputMapping,
                                Map<String, Object> nodeOutput,
                                Context context) {
        if (nodeOutput == null || nodeOutput.isEmpty()) {
            return;
        }
        if (outputMapping == null || outputMapping.isEmpty()) {
            // Fallback: write all output keys to context with their original keys
            for (Map.Entry<String, Object> entry : nodeOutput.entrySet()) {
                context.set(entry.getKey(), entry.getValue());
            }
            return;
        }
        for (Map.Entry<String, String> entry : outputMapping.entrySet()) {
            String outputKey = entry.getKey();
            String contextKey = entry.getValue();
            if (nodeOutput.containsKey(outputKey)) {
                context.set(contextKey, nodeOutput.get(outputKey));
            }
        }
    }
}
