package com.powerflow.workflow.domain.port.outbound;

import com.powerflow.workflow.domain.model.Workflow;
import java.util.List;
import java.util.Optional;

public interface WorkflowRepository {
    Optional<Workflow> findById(String id);
    List<Workflow> findAll();
    List<Workflow> findByEnabled(boolean enabled);
    Workflow save(Workflow workflow);
    void delete(String id);
}
