package com.powerflow.workflow.domain.service.validation;

import com.powerflow.workflow.domain.model.Workflow;
import com.powerflow.workflow.domain.port.outbound.WorkflowRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class WorkflowNameUniquenessValidator implements WorkflowValidator {
    private final WorkflowRepository workflowRepository;

    public WorkflowNameUniquenessValidator(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    @Override
    public void validate(Workflow workflow) {
        List<WorkflowValidationException.ValidationError> errors = new ArrayList<>();

        String workflowName = workflow.getName();
        if (workflowName == null || workflowName.isBlank()) {
            errors.add(new WorkflowValidationException.ValidationError("name", "Workflow name cannot be empty"));
        }

        List<Workflow> existingWorkflows = workflowRepository.findAll();
        boolean nameExists = existingWorkflows.stream()
            .filter(w -> !w.getId().equals(workflow.getId()))
            .anyMatch(w -> w.getName() != null && w.getName().equals(workflowName));

        if (nameExists) {
            errors.add(new WorkflowValidationException.ValidationError("name", "Workflow name must be unique"));
        }

        if (!errors.isEmpty()) {
            throw new WorkflowValidationException(errors);
        }
    }
}
