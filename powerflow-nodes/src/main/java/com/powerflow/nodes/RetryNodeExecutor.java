package com.powerflow.nodes;

import com.powerflow.engine.domain.enums.ExecutionStatus;
import com.powerflow.engine.domain.enums.NodeType;
import com.powerflow.engine.domain.model.Context;
import com.powerflow.engine.domain.model.Node;
import com.powerflow.engine.domain.model.NodeResult;
import com.powerflow.engine.domain.port.outbound.NodeExecutorPort;
import com.powerflow.engine.domain.port.outbound.NodeExecutorCallback;
import com.powerflow.engine.domain.port.outbound.NodeExecutorCallbackSetter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes RETRY nodes - placeholder for retry logic.
 */
public class RetryNodeExecutor implements NodeExecutorPort, NodeExecutorCallbackSetter {

    private NodeExecutorCallback nodeExecutorCallback;

    @Override
    public NodeType supportedType() {
        return NodeType.RETRY;
    }

    @Override
    public void setCallback(NodeExecutorCallback callback) {
        this.nodeExecutorCallback = callback;
    }

    @Override
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
        output.put("note", "RetryNodeExecutor placeholder");

        return NodeResult.builder()
            .nodeId(node.getId())
            .status(ExecutionStatus.SUCCESS)
            .output(output)
            .nextNodeId(nextNodeId)
            .build();
    }
}
