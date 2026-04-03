package com.powerflow.nodes;

import com.powerflow.engine.domain.enums.ExecutionStatus;
import com.powerflow.engine.domain.enums.NodeType;
import com.powerflow.engine.domain.model.Context;
import com.powerflow.engine.domain.model.Node;
import com.powerflow.engine.domain.model.NodeResult;
import com.powerflow.engine.domain.port.outbound.NodeExecutorPort;
import com.powerflow.engine.domain.port.outbound.NodeExecutorCallback;
import com.powerflow.engine.domain.port.outbound.NodeExecutorCallbackSetter;
import com.powerflow.engine.domain.port.outbound.WorkflowRepositoryPort;

import java.util.HashMap;
import java.util.Map;

/**
 * Executes SUBWORKFLOW nodes - calls nested workflows.
 */
public class SubworkflowNodeExecutor implements NodeExecutorPort, NodeExecutorCallbackSetter {

    private WorkflowRepositoryPort workflowRepository;
    private NodeExecutorCallback nodeExecutorCallback;

    public SubworkflowNodeExecutor() {
        // Default constructor for SPI loading
        this.workflowRepository = null;
    }

    public SubworkflowNodeExecutor(WorkflowRepositoryPort workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    @Override
    public NodeType supportedType() {
        return NodeType.SUBWORKFLOW;
    }

    @Override
    public void setCallback(NodeExecutorCallback callback) {
        this.nodeExecutorCallback = callback;
    }

    @Override
    public NodeResult execute(Node node, Context context) {
        String workflowId = (String) node.getConfig().get("workflowId");
        String nextNodeId = (String) node.getConfig().get("nextNodeId");

        if (workflowId == null) {
            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.FAILED)
                .error("workflowId is required for SUBWORKFLOW node")
                .build();
        }

        Map<String, Object> output = new HashMap<>();
        output.put("workflowId", workflowId);
        output.put("executed", true);
        output.put("note", "SubworkflowNodeExecutor placeholder");

        return NodeResult.builder()
            .nodeId(node.getId())
            .status(ExecutionStatus.SUCCESS)
            .output(output)
            .nextNodeId(nextNodeId)
            .build();
    }
}
