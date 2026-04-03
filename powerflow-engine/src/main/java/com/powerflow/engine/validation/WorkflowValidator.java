package com.powerflow.engine.validation;

import com.powerflow.engine.domain.model.Workflow;

/**
 * Workflow validator interface.
 */
public interface WorkflowValidator {
    void validate(Workflow workflow);
}
