package com.powerflow.workflow.domain.service.validation;

import com.powerflow.workflow.domain.model.Edge;
import com.powerflow.workflow.domain.model.Node;
import com.powerflow.workflow.domain.model.Workflow;
import com.powerflow.workflow.domain.model.enums.NodeType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates node in/out degree constraints.
 *
 * Node degree rules:
 * - START: in=0, out=unlimited
 * - END: in=unlimited, out=0
 * - Single I/O nodes (DATA_PROCESSING, HTTP_REQUEST, LLM_CALL, FOREACH, SUBWORKFLOW, TRY_CATCH, RETRY): in=1, out=1
 * - Multi-output nodes (CONDITION, PARALLEL, BRANCH): in=1, out=unlimited
 */
@Component
public class NodeDegreeValidator implements WorkflowValidator {

    private static final Map<NodeType, DegreeConstraint> DEGREE_CONSTRAINTS = new HashMap<>();

    static {
        // START: must have no incoming edges
        DEGREE_CONSTRAINTS.put(NodeType.START, new DegreeConstraint(0, null));

        // END: must have no outgoing edges
        DEGREE_CONSTRAINTS.put(NodeType.END, new DegreeConstraint(null, 0));

        // Single I/O nodes
        DEGREE_CONSTRAINTS.put(NodeType.DATA_PROCESSING, new DegreeConstraint(1, 1));
        DEGREE_CONSTRAINTS.put(NodeType.HTTP_REQUEST, new DegreeConstraint(1, 1));
        DEGREE_CONSTRAINTS.put(NodeType.LLM_CALL, new DegreeConstraint(1, 1));
        DEGREE_CONSTRAINTS.put(NodeType.FOREACH, new DegreeConstraint(1, 1));
        DEGREE_CONSTRAINTS.put(NodeType.SUBWORKFLOW, new DegreeConstraint(1, 1));
        DEGREE_CONSTRAINTS.put(NodeType.TRY_CATCH, new DegreeConstraint(1, 1));
        DEGREE_CONSTRAINTS.put(NodeType.RETRY, new DegreeConstraint(1, 1));

        // Multi-output nodes
        DEGREE_CONSTRAINTS.put(NodeType.CONDITION, new DegreeConstraint(1, null));
        DEGREE_CONSTRAINTS.put(NodeType.PARALLEL, new DegreeConstraint(1, null));
        DEGREE_CONSTRAINTS.put(NodeType.BRANCH, new DegreeConstraint(1, null));
    }

    private record DegreeConstraint(Integer maxInDegree, Integer maxOutDegree) {}

    @Override
    public void validate(Workflow workflow) {
        List<WorkflowValidationException.ValidationError> errors = new ArrayList<>();

        if (workflow.getNodes() == null || workflow.getNodes().isEmpty()) {
            return; // Already validated by WorkflowStructureValidator
        }

        // Compute in/out degrees for each node
        Map<String, Integer> inDegrees = new HashMap<>();
        Map<String, Integer> outDegrees = new HashMap<>();

        workflow.getNodes().keySet().forEach(id -> {
            inDegrees.put(id, 0);
            outDegrees.put(id, 0);
        });

        for (Edge edge : workflow.getEdges()) {
            inDegrees.merge(edge.getToNodeId(), 1, Integer::sum);
            outDegrees.merge(edge.getFromNodeId(), 1, Integer::sum);
        }

        // Validate each node's degree
        for (Map.Entry<String, Node> entry : workflow.getNodes().entrySet()) {
            String nodeId = entry.getKey();
            Node node = entry.getValue();
            NodeType nodeType = resolveNodeType(node);

            DegreeConstraint constraint = DEGREE_CONSTRAINTS.get(nodeType);
            if (constraint == null) {
                // Unknown node type, skip validation
                continue;
            }

            int inDegree = inDegrees.getOrDefault(nodeId, 0);
            int outDegree = outDegrees.getOrDefault(nodeId, 0);

            // Check in-degree constraint
            if (constraint.maxInDegree() != null && inDegree > constraint.maxInDegree()) {
                String nodeName = node.getName() != null ? node.getName() : nodeId;
                String message = String.format(
                    "节点 '%s' (类型: %s) 入度为 %d，但允许的最大入度为 %d",
                    nodeName, nodeType, inDegree, constraint.maxInDegree()
                );
                errors.add(new WorkflowValidationException.ValidationError("nodes", message));
            }

            // Check out-degree constraint
            if (constraint.maxOutDegree() != null && outDegree > constraint.maxOutDegree()) {
                String nodeName = node.getName() != null ? node.getName() : nodeId;
                String message = String.format(
                    "节点 '%s' (类型: %s) 出度为 %d，但允许的最大出度为 %d",
                    nodeName, nodeType, outDegree, constraint.maxOutDegree()
                );
                errors.add(new WorkflowValidationException.ValidationError("nodes", message));
            }
        }

        validateParallelStructure(workflow, errors);

        if (!errors.isEmpty()) {
            throw new WorkflowValidationException(errors);
        }
    }

    private NodeType resolveNodeType(Node node) {
        if (node.getType() != null) {
            return node.getType();
        }
        // Fallback: try to parse from type name string
        String typeName = node.getType() != null ? node.getType().name() : null;
        if (typeName == null) {
            return NodeType.DATA_PROCESSING; // default
        }
        try {
            return NodeType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return NodeType.DATA_PROCESSING; // default for unknown types
        }
    }

    private void validateParallelStructure(Workflow workflow, List<WorkflowValidationException.ValidationError> errors) {
        // Find all PARALLEL nodes
        for (Node node : workflow.getNodes().values()) {
            if (node.getType() != NodeType.PARALLEL) continue;

            String nodeId = node.getId();

            // Find outgoing edges from PARALLEL
            List<Edge> outgoingEdges = workflow.getEdges().stream()
                .filter(e -> e.getFromNodeId().equals(nodeId))
                .toList();

            // PARALLEL must have edges to branches AND exactly one edge to JOIN
            long branchEdges = outgoingEdges.size();
            if (branchEdges < 2) {
                errors.add(new WorkflowValidationException.ValidationError("nodes",
                    String.format("PARALLEL node '%s' must have at least 2 branch connections", nodeId)));
            }

            // Find the JOIN node
            List<Edge> joinEdges = outgoingEdges.stream()
                .filter(e -> isJoinNode(workflow, e.getToNodeId()))
                .toList();

            if (joinEdges.isEmpty()) {
                errors.add(new WorkflowValidationException.ValidationError("nodes",
                    String.format("PARALLEL node '%s' must connect to a JOIN node", nodeId)));
            } else if (joinEdges.size() > 1) {
                errors.add(new WorkflowValidationException.ValidationError("nodes",
                    String.format("PARALLEL node '%s' must connect to exactly one JOIN node", nodeId)));
            }
        }
    }

    private boolean isJoinNode(Workflow workflow, String nodeId) {
        long incomingEdges = workflow.getEdges().stream()
            .filter(e -> e.getToNodeId().equals(nodeId))
            .count();
        return incomingEdges > 1;
    }
}
