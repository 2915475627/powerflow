package com.powerflow.workflow.domain.model;

import com.powerflow.workflow.domain.model.enums.NodeType;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class WorkflowTest {

    @Test
    void should_build_workflow_and_retrieve_nodes() {
        Node node1 = Node.builder().id("node-1").name("Start").type(NodeType.DATA_PROCESSING).build();
        Node node2 = Node.builder().id("node-2").name("End").type(NodeType.DATA_PROCESSING).build();
        Edge edge = Edge.builder().id("edge-1").fromNodeId("node-1").toNodeId("node-2").build();

        Workflow workflow = Workflow.builder()
            .id("wf-1")
            .name("Simple Workflow")
            .startNodeId("node-1")
            .nodes(List.of(node1, node2))
            .edges(List.of(edge))
            .build();

        assertThat(workflow.getId()).isEqualTo("wf-1");
        assertThat(workflow.findNodeById("node-1")).isPresent();
        assertThat(workflow.findNodeById("node-2")).isPresent();
        assertThat(workflow.findEdgesByFromNodeId("node-1")).hasSize(1);
    }

    @Test
    void should_return_empty_when_node_not_found() {
        Workflow workflow = Workflow.builder()
            .id("wf-1")
            .startNodeId("node-1")
            .nodes(List.of())
            .edges(List.of())
            .build();

        assertThat(workflow.findNodeById("non-existent")).isEmpty();
    }
}
