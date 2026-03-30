package com.powerflow.workflow.domain.model;

import com.powerflow.workflow.domain.model.enums.NodeType;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class NodeTest {

    @Test
    void should_build_node_with_all_fields() {
        Node node = Node.builder()
            .id("node-1")
            .name("Calculate Discount")
            .type(NodeType.DATA_PROCESSING)
            .config(Map.of("transform", "input.amount * 0.9", "outputKey", "discountedAmount"))
            .inputMapping(Map.of("amount", "input.amount"))
            .outputMapping(Map.of("discountedAmount", "output.discountedAmount"))
            .build();

        assertThat(node.getId()).isEqualTo("node-1");
        assertThat(node.getName()).isEqualTo("Calculate Discount");
        assertThat(node.getType()).isEqualTo(NodeType.DATA_PROCESSING);
        assertThat(node.getConfig()).containsEntry("transform", "input.amount * 0.9");
        assertThat(node.getInputMapping()).containsEntry("amount", "input.amount");
        assertThat(node.getOutputMapping()).containsEntry("discountedAmount", "output.discountedAmount");
    }

    @Test
    void should_return_defensive_copies() {
        Node node = Node.builder()
            .id("node-1")
            .type(NodeType.DATA_PROCESSING)
            .build();

        node.getConfig().put("key", "value");
        node.getInputMapping().put("key", "value");
        node.getOutputMapping().put("key", "value");

        assertThat(node.getConfig()).isEmpty();
        assertThat(node.getInputMapping()).isEmpty();
        assertThat(node.getOutputMapping()).isEmpty();
    }
}
