package com.powerflow.nodes;

import com.powerflow.engine.domain.enums.ExecutionStatus;
import com.powerflow.engine.domain.enums.NodeType;
import com.powerflow.engine.domain.model.Context;
import com.powerflow.engine.domain.model.Node;
import com.powerflow.engine.domain.model.NodeResult;
import com.powerflow.engine.domain.port.outbound.NodeExecutorPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes PARALLEL nodes - marks parallel branches.
 * Actual parallel execution is handled by the WorkflowEngine.
 */
public class ParallelNodeExecutor implements NodeExecutorPort {

    @Override
    public NodeType supportedType() {
        return NodeType.PARALLEL;
    }

    @Override
    public NodeResult execute(Node node, Context context) {
        try {
            String strategy = (String) node.getConfig().getOrDefault("strategy", "AND");
            String nextNodeId = (String) node.getConfig().get("nextNodeId");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> branches = (List<Map<String, Object>>) node.getConfig().get("branches");
            List<Map<String, Object>> branchResults = new ArrayList<>();

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
