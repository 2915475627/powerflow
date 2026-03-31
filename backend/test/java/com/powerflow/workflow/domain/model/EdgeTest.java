package com.powerflow.workflow.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EdgeTest {

    @Test
    void should_build_edge_without_condition() {
        Edge edge = Edge.builder()
            .id("edge-1")
            .fromNodeId("node-1")
            .toNodeId("node-2")
            .build();

        assertThat(edge.getId()).isEqualTo("edge-1");
        assertThat(edge.hasCondition()).isFalse();
    }

    @Test
    void should_build_edge_with_condition() {
        Edge edge = Edge.builder()
            .id("edge-2")
            .fromNodeId("node-1")
            .toNodeId("node-3")
            .condition("input.amount > 1000")
            .build();

        assertThat(edge.hasCondition()).isTrue();
        assertThat(edge.getCondition()).isEqualTo("input.amount > 1000");
    }
}
