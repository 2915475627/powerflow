package com.powerflow.workflow.domain.service;

import com.powerflow.workflow.domain.model.Context;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class ContextManager {

    public Map<String, Object> extractNodeInput(Map<String, String> inputMapping, Context context) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, String> entry : inputMapping.entrySet()) {
            String contextKey = entry.getValue().replace("input.", "");
            context.get(contextKey).ifPresent(value -> result.put(entry.getKey(), value));
        }
        return result;
    }

    public void writeNodeOutput(Map<String, String> outputMapping,
                                 Map<String, Object> nodeOutput,
                                 Context context) {
        for (Map.Entry<String, String> entry : outputMapping.entrySet()) {
            String outputKey = entry.getKey();
            String contextKey = entry.getValue();
            if (nodeOutput.containsKey(outputKey)) {
                context.set(contextKey, nodeOutput.get(outputKey));
            }
        }
    }
}