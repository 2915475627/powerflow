package com.powerflow.workflow.domain.service.validation;

import com.powerflow.workflow.domain.model.Workflow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class WorkflowStructureValidator implements WorkflowValidator {

    @Override
    public void validate(Workflow workflow) {
        List<WorkflowValidationException.ValidationError> errors = new ArrayList<>();

        if (workflow.getNodes() == null || workflow.getNodes().isEmpty()) {
            errors.add(new WorkflowValidationException.ValidationError("nodes", "Workflow must have at least one node"));
        }

        if (!errors.isEmpty()) {
            throw new WorkflowValidationException(errors);
        }
    }
}
