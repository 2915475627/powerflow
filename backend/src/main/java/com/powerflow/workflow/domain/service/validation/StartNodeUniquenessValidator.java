package com.powerflow.workflow.domain.service.validation;

import com.powerflow.workflow.domain.model.Node;
import com.powerflow.workflow.domain.model.Workflow;
import com.powerflow.workflow.domain.model.enums.NodeType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StartNodeUniquenessValidator implements WorkflowValidator {

    @Override
    public void validate(Workflow workflow) {
        List<WorkflowValidationException.ValidationError> errors = new ArrayList<>();

        if (workflow.getNodes() == null || workflow.getNodes().isEmpty()) {
            errors.add(new WorkflowValidationException.ValidationError("nodes", "Workflow must have at least one node"));
            throw new WorkflowValidationException(errors);
        }

        // Check for START node by enum type OR by string type (for deserialization flexibility)
        long startNodeCount = workflow.getNodes().values().stream()
            .filter(node -> {
                if (node.getType() == NodeType.START) return true;
                // Also check if type name contains START (fallback for string-based deserialization)
                if (node.getType() != null && node.getType().name().contains("START")) return true;
                return false;
            })
            .count();

        if (startNodeCount == 0) {
            errors.add(new WorkflowValidationException.ValidationError("nodes", "Workflow must have at least one START node"));
        } else if (startNodeCount > 1) {
            errors.add(new WorkflowValidationException.ValidationError("nodes", "Workflow can only have one START node"));
        }

        if (!errors.isEmpty()) {
            throw new WorkflowValidationException(errors);
        }
    }
}
