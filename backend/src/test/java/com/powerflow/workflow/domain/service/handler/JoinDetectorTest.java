package com.powerflow.workflow.domain.service.handler;

import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.model.enums.NodeType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JoinDetectorTest {

    @Test
    void shouldDetectJoinNodeWithMultipleIncomingEdges() {
        // Workflow: A → JOIN ← B
        Workflow workflow = Workflow.builder()
            .id("test")
            .name("Test")
            .nodes(List.of(
                Node.builder().id("A").type(NodeType.DATA_PROCESSING).build(),
                Node.builder().id("B").type(NodeType.DATA_PROCESSING).build(),
                Node.builder().id("JOIN").type(NodeType.JOIN).build()
            ))
            .edges(List.of(
                Edge.builder().id("e1").fromNodeId("A").toNodeId("JOIN").build(),
                Edge.builder().id("e2").fromNodeId("B").toNodeId("JOIN").build()
            ))
            .build();

        JoinDetector detector = new JoinDetector();

        assertTrue(detector.isJoinNode(workflow, "JOIN"));
        assertFalse(detector.isJoinNode(workflow, "A"));
        assertFalse(detector.isJoinNode(workflow, "B"));
    }

    @Test
    void shouldFindBranchesForJoin() {
        Workflow workflow = Workflow.builder()
            .id("test")
            .name("Test")
            .nodes(List.of(
                Node.builder().id("A").type(NodeType.DATA_PROCESSING).build(),
                Node.builder().id("B").type(NodeType.DATA_PROCESSING).build(),
                Node.builder().id("JOIN").type(NodeType.JOIN).build()
            ))
            .edges(List.of(
                Edge.builder().id("e1").fromNodeId("A").toNodeId("JOIN").build(),
                Edge.builder().id("e2").fromNodeId("B").toNodeId("JOIN").build()
            ))
            .build();

        JoinDetector detector = new JoinDetector();
        List<String> branches = detector.findBranchesForJoin(workflow, "JOIN");

        assertEquals(2, branches.size());
        assertTrue(branches.contains("A"));
        assertTrue(branches.contains("B"));
    }
}