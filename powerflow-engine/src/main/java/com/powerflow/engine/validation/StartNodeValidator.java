package com.powerflow.engine.validation;

import com.powerflow.engine.domain.enums.NodeType;
import com.powerflow.engine.domain.model.Workflow;
import com.powerflow.engine.exception.WorkflowValidationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that the workflow has exactly one START node.
 */
public class StartNodeValidator implements WorkflowValidator {

    @Override
    public void validate(Workflow workflow) {
        List<WorkflowValidationException.ValidationError> errors = new ArrayList<>();

        long startNodeCount = workflow.getNodes().values().stream()
            .filter(n -> n.getType() == NodeType.START)
            .count();

        if (startNodeCount == 0) {
            errors.add(new WorkflowValidationException.ValidationError("startNodeId", "Workflow must have a START node"));
        } else if (startNodeCount > 1) {
            errors.add(new WorkflowValidationException.ValidationError("nodes", "Workflow must have exactly one START node"));
        }

        if (workflow.getStartNodeId() == null || workflow.getStartNodeId().isBlank()) {
            errors.add(new WorkflowValidationException.ValidationError("startNodeId", "Workflow startNodeId must be set"));
        }

        if (!errors.isEmpty()) {
            throw new WorkflowValidationException(errors);
        }
    }
}
