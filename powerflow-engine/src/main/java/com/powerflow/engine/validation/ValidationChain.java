package com.powerflow.engine.validation;

import com.powerflow.engine.domain.model.Workflow;

/**
 * Validation chain interface.
 */
public interface ValidationChain {
    void validate(Workflow workflow);
}
