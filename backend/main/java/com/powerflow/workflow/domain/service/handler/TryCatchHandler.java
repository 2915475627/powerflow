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
public class TryCatchHandler {

    private NodeExecutorCallback nodeExecutorCallback;

    public void setNodeExecutorCallback(NodeExecutorCallback callback) {
        this.nodeExecutorCallback = callback;
    }

    @SuppressWarnings("unchecked")
    public NodeResult execute(Node node, Context context) {
        List<String> tryNodeIds = (List<String>) node.getConfig().get("tryNodes");
        List<String> catchNodeIds = (List<String>) node.getConfig().get("catchNodes");
        String nextNodeId = (String) node.getConfig().get("nextNodeId");

        Map<String, Object> output = new HashMap<>();
        output.put("tryNodes", tryNodeIds);
        output.put("catchNodes", catchNodeIds);
        output.put("executed", true);
        output.put("note", "TryCatchHandler placeholder - subgraph execution requires workflow context");

        return NodeResult.builder()
            .nodeId(node.getId())
            .status(ExecutionStatus.SUCCESS)
            .output(output)
            .nextNodeId(nextNodeId)
            .build();
    }
}
