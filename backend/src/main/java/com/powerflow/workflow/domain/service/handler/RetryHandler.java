package com.powerflow.workflow.domain.service.handler;

import com.powerflow.workflow.domain.model.Context;
import com.powerflow.workflow.domain.model.Node;
import com.powerflow.workflow.domain.model.NodeResult;
import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import com.powerflow.workflow.domain.port.outbound.NodeExecutorCallback;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RetryHandler {

    private NodeExecutorCallback nodeExecutorCallback;

    public void setNodeExecutorCallback(NodeExecutorCallback callback) {
        this.nodeExecutorCallback = callback;
    }

    @SuppressWarnings("unchecked")
    public NodeResult execute(Node node, Context context) {
        int maxAttempts = (Integer) node.getConfig().getOrDefault("maxAttempts", 3);
        long initialDelayMs = ((Number) node.getConfig().getOrDefault("initialDelayMs", 1000)).longValue();
        String backoffStrategy = (String) node.getConfig().getOrDefault("backoffStrategy", "EXPONENTIAL");
        List<String> targetNodeIds = (List<String>) node.getConfig().get("targetNodeIds");
        String nextNodeId = (String) node.getConfig().get("nextNodeId");

        Map<String, Object> output = new HashMap<>();
        output.put("maxAttempts", maxAttempts);
        output.put("initialDelayMs", initialDelayMs);
        output.put("backoffStrategy", backoffStrategy);
        output.put("targetNodes", targetNodeIds);
        output.put("executed", true);
        output.put("note", "RetryHandler placeholder - subgraph execution requires workflow context");

        return NodeResult.builder()
            .nodeId(node.getId())
            .status(ExecutionStatus.SUCCESS)
            .output(output)
            .nextNodeId(nextNodeId)
            .build();
    }
}
