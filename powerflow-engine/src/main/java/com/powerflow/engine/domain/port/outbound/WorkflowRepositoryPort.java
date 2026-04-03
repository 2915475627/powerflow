package com.powerflow.engine.domain.port.outbound;

import com.powerflow.engine.domain.model.Workflow;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for workflow persistence.
 */
public interface WorkflowRepositoryPort {
    Optional<Workflow> findById(String id);
    List<Workflow> findAll();
    List<Workflow> findByEnabled(boolean enabled);
    Workflow save(Workflow workflow);
    void delete(String id);
}
