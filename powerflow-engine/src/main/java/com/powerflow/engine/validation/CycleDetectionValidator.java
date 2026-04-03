package com.powerflow.engine.validation;

import com.powerflow.engine.domain.model.Workflow;
import com.powerflow.engine.domain.model.Edge;
import com.powerflow.engine.exception.WorkflowValidationException;

import java.util.*;

/**
 * Detects cycles in the workflow DAG using DFS.
 */
public class CycleDetectionValidator implements WorkflowValidator {

    @Override
    public void validate(Workflow workflow) {
        // Build adjacency list
        Map<String, List<String>> dag = new HashMap<>();
        for (String nodeId : workflow.getNodes().keySet()) {
            dag.put(nodeId, new ArrayList<>());
        }
        for (Edge edge : workflow.getEdges()) {
            dag.computeIfAbsent(edge.getFromNodeId(), k -> new ArrayList<>()).add(edge.getToNodeId());
        }

        // DFS-based cycle detection
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();

        for (String nodeId : workflow.getNodes().keySet()) {
            if (!visited.contains(nodeId)) {
                if (hasCycle(nodeId, dag, visiting, visited)) {
                    throw new WorkflowValidationException(List.of(
                        new WorkflowValidationException.ValidationError("edges", "Workflow contains a cycle")
                    ));
                }
            }
        }
    }

    private boolean hasCycle(String nodeId, Map<String, List<String>> dag,
                             Set<String> visiting, Set<String> visited) {
        if (visiting.contains(nodeId)) {
            return true; // Cycle detected
        }
        if (visited.contains(nodeId)) {
            return false;
        }

        visiting.add(nodeId);

        List<String> neighbors = dag.getOrDefault(nodeId, List.of());
        for (String neighbor : neighbors) {
            if (hasCycle(neighbor, dag, visiting, visited)) {
                return true;
            }
        }

        visiting.remove(nodeId);
        visited.add(nodeId);
        return false;
    }
}
