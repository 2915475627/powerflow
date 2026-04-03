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
     */
    public void writeNodeOutput(Map<String, String> outputMapping,
                                Map<String, Object> nodeOutput,
                                Context context) {
        if (outputMapping == null || outputMapping.isEmpty()) {
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
