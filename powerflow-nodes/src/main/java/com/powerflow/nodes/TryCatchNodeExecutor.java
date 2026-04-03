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
 * Executes TRY_CATCH nodes - placeholder for try-catch error handling.
 */
public class TryCatchNodeExecutor implements NodeExecutorPort, NodeExecutorCallbackSetter {

    private NodeExecutorCallback nodeExecutorCallback;

    @Override
    public NodeType supportedType() {
        return NodeType.TRY_CATCH;
    }

    @Override
    public void setCallback(NodeExecutorCallback callback) {
        this.nodeExecutorCallback = callback;
    }

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult execute(Node node, Context context) {
        List<String> tryNodeIds = (List<String>) node.getConfig().get("tryNodes");
        List<String> catchNodeIds = (List<String>) node.getConfig().get("catchNodes");
        String nextNodeId = (String) node.getConfig().get("nextNodeId");

        Map<String, Object> output = new HashMap<>();
        output.put("tryNodes", tryNodeIds);
        output.put("catchNodes", catchNodeIds);
        output.put("executed", true);
        output.put("note", "TryCatchNodeExecutor placeholder");

        return NodeResult.builder()
            .nodeId(node.getId())
            .status(ExecutionStatus.SUCCESS)
            .output(output)
            .nextNodeId(nextNodeId)
            .build();
    }
}
