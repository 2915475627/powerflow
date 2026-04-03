package com.powerflow.engine.validation;

import com.powerflow.engine.domain.model.Workflow;
import com.powerflow.engine.exception.WorkflowValidationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates basic workflow structure.
 */
public class WorkflowStructureValidator implements WorkflowValidator {

    @Override
    public void validate(Workflow workflow) {
        List<WorkflowValidationException.ValidationError> errors = new ArrayList<>();

        if (workflow.getNodes() == null || workflow.getNodes().isEmpty()) {
            errors.add(new WorkflowValidationException.ValidationError("nodes", "Workflow must have at least one node"));
        }

        if (workflow.getEdges() == null || workflow.getEdges().isEmpty()) {
            errors.add(new WorkflowValidationException.ValidationError("edges", "Workflow must have at least one edge"));
        }

        if (!errors.isEmpty()) {
            throw new WorkflowValidationException(errors);
        }
    }
}
