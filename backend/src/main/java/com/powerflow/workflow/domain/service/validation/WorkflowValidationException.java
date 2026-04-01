package com.powerflow.workflow.domain.service.validation;

import java.util.List;

public class WorkflowValidationException extends RuntimeException {
    private final List<ValidationError> errors;

    public WorkflowValidationException(List<ValidationError> errors) {
        super("Workflow validation failed");
        this.errors = errors;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public static class ValidationError {
        private final String field;
        private final String message;

        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() { return field; }
        public String getMessage() { return message; }
    }
}
