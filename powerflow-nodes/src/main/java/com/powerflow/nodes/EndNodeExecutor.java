package com.powerflow.nodes;

import com.powerflow.engine.domain.enums.ExecutionStatus;
import com.powerflow.engine.domain.enums.NodeType;
import com.powerflow.engine.domain.model.Context;
import com.powerflow.engine.domain.model.Node;
import com.powerflow.engine.domain.model.NodeResult;
import com.powerflow.engine.domain.port.outbound.NodeExecutorPort;

import java.util.Map;

/**
 * Executes END nodes - terminates workflow execution.
 */
public class EndNodeExecutor implements NodeExecutorPort {

    @Override
    public NodeType supportedType() {
        return NodeType.END;
    }

    @Override
    public NodeResult execute(Node node, Context context) {
        return NodeResult.builder()
            .nodeId(node.getId())
            .status(ExecutionStatus.SUCCESS)
            .output(Map.of())
            .nextNodeId(null) // End node - no next node
            .build();
    }
}
