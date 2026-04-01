package com.powerflow.workflow.domain.service.handler;

import com.powerflow.workflow.domain.model.Edge;
import com.powerflow.workflow.domain.model.Workflow;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Detects JOIN nodes based on graph structure.
 * A JOIN node is one that:
 * 1. Has multiple incoming edges (from parallel branches)
 * 2. Has exactly one outgoing edge (to continuation)
 */
@Component
public class JoinDetector {

    /**
     * Find all JOIN nodes in the workflow.
     * A node is a JOIN if it has more than one incoming edge.
     */
    public List<String> findJoinNodes(Workflow workflow) {
        Map<String, Long> inDegree = computeInDegrees(workflow);

        return inDegree.entrySet().stream()
            .filter(e -> e.getValue() > 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Check if a specific node is a JOIN node.
     */
    public boolean isJoinNode(Workflow workflow, String nodeId) {
        long incomingEdges = workflow.getEdges().stream()
            .filter(e -> e.getToNodeId().equals(nodeId))
            .count();
        return incomingEdges > 1;
    }

    /**
     * Find the JOIN node that a PARALLEL node connects to.
     * PARALLEL should have exactly one edge to a JOIN node.
     */
    public String findJoinForParallel(Workflow workflow, String parallelNodeId) {
        return workflow.getEdges().stream()
            .filter(e -> e.getFromNodeId().equals(parallelNodeId))
            .filter(e -> isJoinNode(workflow, e.getToNodeId()))
            .map(Edge::getToNodeId)
            .findFirst()
            .orElse(null);
    }

    /**
     * Find all branch nodes that feed into a JOIN.
     * These are all nodes (except PARALLEL) that have edges to the JOIN.
     */
    public List<String> findBranchesForJoin(Workflow workflow, String joinNodeId) {
        return workflow.getEdges().stream()
            .filter(e -> e.getToNodeId().equals(joinNodeId))
            .map(Edge::getFromNodeId)
            .filter(nodeId -> !isParallelNode(workflow, nodeId))
            .collect(Collectors.toList());
    }

    private boolean isParallelNode(Workflow workflow, String nodeId) {
        return workflow.findNodeById(nodeId)
            .map(n -> n.getType() != null && n.getType().name().equals("PARALLEL"))
            .orElse(false);
    }

    private Map<String, Long> computeInDegrees(Workflow workflow) {
        return workflow.getEdges().stream()
            .collect(Collectors.groupingBy(
                Edge::getToNodeId,
                Collectors.counting()
            ));
    }
}