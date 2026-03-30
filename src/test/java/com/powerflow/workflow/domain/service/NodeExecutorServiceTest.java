package com.powerflow.workflow.domain.service;

import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import com.powerflow.workflow.domain.model.enums.NodeType;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class NodeExecutorServiceTest {

    @Test
    void should_execute_data_processing_node() {
        NodeExecutorService executor = new NodeExecutorService(null);
        Context ctx = new Context(Map.of("amount", 1000));

        Node node = Node.builder()
            .id("node-1")
            .type(NodeType.DATA_PROCESSING)
            .config(Map.of("outputKey", "result", "expression", "#input.amount * 0.9"))
            .build();

        NodeResult result = executor.execute(node, ctx);

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(result.getOutput()).containsEntry("result", 900.0);
    }

    @Test
    void should_execute_condition_node_and_return_next_node() {
        NodeExecutorService executor = new NodeExecutorService(null);
        Context ctx = new Context(Map.of("amount", 1500));

        Node conditionNode = Node.builder()
            .id("condition-1")
            .type(NodeType.CONDITION)
            .config(Map.of(
                "conditions", java.util.List.of(
                    Map.of("expression", "#input.amount > 1000", "nextNodeId", "node-high"),
                    Map.of("expression", "#input.amount > 500", "nextNodeId", "node-medium")
                ),
                "defaultNextNodeId", "node-low"
            ))
            .build();

        NodeResult result = executor.execute(conditionNode, ctx);

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(result.getNextNodeId()).containsValue("node-high");
    }
}