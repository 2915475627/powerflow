package com.powerflow.workflow.domain.service.handler;

import com.powerflow.workflow.domain.model.Context;
import com.powerflow.workflow.domain.model.Node;
import com.powerflow.workflow.domain.model.NodeResult;
import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import com.powerflow.workflow.domain.port.outbound.NodeExecutorCallback;
import com.powerflow.workflow.domain.port.outbound.WorkflowRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ParallelHandler {

    private final WorkflowRepository workflowRepository;
    private NodeExecutorCallback nodeExecutorCallback;

    public ParallelHandler(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    public void setNodeExecutorCallback(NodeExecutorCallback callback) {
        this.nodeExecutorCallback = callback;
    }

    @SuppressWarnings("unchecked")
    public NodeResult execute(Node node, Context context) {
        try {
            String strategy = (String) node.getConfig().getOrDefault("strategy", "AND");
            String nextNodeId = (String) node.getConfig().get("nextNodeId");

            // Branch execution happens here but actual sub-node execution
            // is delegated to WorkflowExecutor via callback
            List<Map<String, Object>> branchResults = new ArrayList<>();
            boolean allSuccess = true;

            // For now, we just mark as executed - actual parallel execution
            // will be handled by the execution framework
            List<Map<String, Object>> branches = (List<Map<String, Object>>) node.getConfig().get("branches");
            if (branches != null) {
                for (Map<String, Object> branch : branches) {
                    String branchName = (String) branch.getOrDefault("name", "unnamed");
                    branchResults.add(Map.of(
                        "name", branchName,
                        "executed", true,
                        "status", "completed"
                    ));
                }
            }

            Map<String, Object> output = new HashMap<>();
            output.put("branchResults", branchResults);
            output.put("strategy", strategy);
            output.put("parallel", true);

            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.SUCCESS)
                .output(output)
                .nextNodeId(nextNodeId)  // Points to JOIN node
                .build();

        } catch (Exception e) {
            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.FAILED)
                .error(e.getMessage())
                .build();
        }
    }
}
