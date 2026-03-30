package com.powerflow.workflow.domain.port.outbound;

import com.powerflow.workflow.domain.model.Workflow;
import java.util.Optional;

public interface WorkflowRepository {
    Optional<Workflow> findById(String id);
    Workflow save(Workflow workflow);
    void delete(String id);
}
