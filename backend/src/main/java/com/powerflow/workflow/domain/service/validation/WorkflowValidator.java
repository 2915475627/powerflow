package com.powerflow.workflow.domain.service.validation;

import com.powerflow.workflow.domain.model.Workflow;

public interface WorkflowValidator {
    void validate(Workflow workflow);
}
