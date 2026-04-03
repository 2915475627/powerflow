package com.powerflow.web.config;

import com.powerflow.engine.domain.model.Workflow;
import com.powerflow.engine.validation.CycleDetectionValidator;
import com.powerflow.engine.validation.StartNodeValidator;
import com.powerflow.engine.validation.ValidationChain;
import com.powerflow.engine.validation.WorkflowStructureValidator;
import com.powerflow.engine.validation.WorkflowValidator;

import java.util.List;

public class DefaultValidationChain implements ValidationChain {

    private final List<WorkflowValidator> validators;

    public DefaultValidationChain() {
        this.validators = List.of(
            new StartNodeValidator(),
            new WorkflowStructureValidator(),
            new CycleDetectionValidator()
        );
    }

    @Override
    public void validate(Workflow workflow) {
        for (WorkflowValidator validator : validators) {
            validator.validate(workflow);
        }
    }
}
