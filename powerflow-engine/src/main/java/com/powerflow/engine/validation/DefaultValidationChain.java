package com.powerflow.engine.validation;

import com.powerflow.engine.domain.model.Workflow;

import java.util.List;

/**
 * Default validation chain with built-in validators.
 */
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
