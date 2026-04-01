package com.powerflow.workflow.domain.service.validation;

import com.powerflow.workflow.domain.model.Node;
import com.powerflow.workflow.domain.model.Workflow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class NodeNameUniquenessValidator implements WorkflowValidator {

    @Override
    public void validate(Workflow workflow) {
        List<WorkflowValidationException.ValidationError> errors = new ArrayList<>();

        Set<String> nodeNames = new HashSet<>();
        for (Node node : workflow.getNodes().values()) {
            String name = node.getName();
            if (name == null || name.isBlank()) {
                errors.add(new WorkflowValidationException.ValidationError(
                    "nodes." + node.getId() + ".name",
                    "Node name cannot be empty"
                ));
            } else if (!nodeNames.add(name)) {
                errors.add(new WorkflowValidationException.ValidationError(
                    "nodes." + node.getId() + ".name",
                    "Node name must be unique: " + name
                ));
            }
        }

        if (!errors.isEmpty()) {
            throw new WorkflowValidationException(errors);
        }
    }
}
