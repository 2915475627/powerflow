package com.powerflow.workflow.domain.service.handler;

import com.powerflow.workflow.domain.model.Context;
import com.powerflow.workflow.domain.model.Node;
import com.powerflow.workflow.domain.model.NodeResult;
import com.powerflow.workflow.domain.model.WorkflowExecutionResult;
import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import com.powerflow.workflow.domain.port.outbound.NodeExecutorCallback;
import com.powerflow.workflow.domain.port.outbound.WorkflowRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SubworkflowHandler {

    private final WorkflowRepository workflowRepository;
    private NodeExecutorCallback nodeExecutorCallback;

    public SubworkflowHandler(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    public void setNodeExecutorCallback(NodeExecutorCallback callback) {
        this.nodeExecutorCallback = callback;
    }

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
        output.put("note", "SubworkflowHandler placeholder - actual subworkflow execution requires WorkflowExecutor reference");

        return NodeResult.builder()
            .nodeId(node.getId())
            .status(ExecutionStatus.SUCCESS)
            .output(output)
            .nextNodeId(nextNodeId)
            .build();
    }
}
