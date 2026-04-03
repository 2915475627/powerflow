package com.powerflow.nodes;

import com.powerflow.engine.domain.enums.ExecutionStatus;
import com.powerflow.engine.domain.enums.NodeType;
import com.powerflow.engine.domain.model.Context;
import com.powerflow.engine.domain.model.Node;
import com.powerflow.engine.domain.model.NodeResult;
import com.powerflow.engine.domain.port.outbound.NodeExecutorPort;

import java.util.Map;

/**
 * Executes START nodes - passes through to next node.
 */
public class StartNodeExecutor implements NodeExecutorPort {

    @Override
    public NodeType supportedType() {
        return NodeType.START;
    }

    @Override
    public NodeResult execute(Node node, Context context) {
        String nextNodeId = (String) node.getConfig().get("nextNodeId");
        return NodeResult.builder()
            .nodeId(node.getId())
            .status(ExecutionStatus.SUCCESS)
            .output(Map.of())
            .nextNodeId(nextNodeId)
            .build();
    }
}
