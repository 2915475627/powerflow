package com.powerflow.workflow.domain.service.validation;

import com.powerflow.workflow.domain.model.Workflow;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultWorkflowValidationChain implements WorkflowValidationChain {
    private final List<WorkflowValidator> validators;

    public DefaultWorkflowValidationChain(List<WorkflowValidator> validators) {
        this.validators = validators;
    }

    @Override
    public void validate(Workflow workflow) {
        for (WorkflowValidator validator : validators) {
            validator.validate(workflow);
        }
    }
}
