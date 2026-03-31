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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

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
            List<Map<String, Object>> branches = (List<Map<String, Object>>) node.getConfig().get("branches");
            String strategy = (String) node.getConfig().getOrDefault("strategy", "AND");
            String nextNodeId = (String) node.getConfig().get("nextNodeId");

            if (branches == null || branches.isEmpty()) {
                return NodeResult.builder()
                    .nodeId(node.getId())
                    .status(ExecutionStatus.SUCCESS)
                    .output(Map.of("note", "No branches configured"))
                    .nextNodeId(nextNodeId)
                    .build();
            }

            List<Map<String, Object>> branchResults = new ArrayList<>();
            ForkJoinPool pool = ForkJoinPool.commonPool();

            for (Map<String, Object> branch : branches) {
                String branchName = (String) branch.getOrDefault("name", "unnamed");
                @SuppressWarnings("unchecked")
                List<String> nodeIds = (List<String>) branch.get("nodeIds");

                branchResults.add(Map.of(
                    "name", branchName,
                    "nodeIds", nodeIds,
                    "executed", true,
                    "note", "ParallelHandler placeholder - actual parallel execution requires workflow context"
                ));
            }

            Map<String, Object> output = new HashMap<>();
            output.put("branchResults", branchResults);
            output.put("strategy", strategy);
            output.put("parallel", true);

            boolean allSuccess = true;

            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.SUCCESS)
                .output(output)
                .nextNodeId(nextNodeId)
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
