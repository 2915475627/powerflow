package com.powerflow.web.persistence;

import com.powerflow.engine.domain.model.Edge;
import com.powerflow.engine.domain.model.Node;
import com.powerflow.engine.domain.model.Workflow;
import com.powerflow.engine.domain.port.outbound.WorkflowRepositoryPort;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryWorkflowRepository implements WorkflowRepositoryPort {

    private final Map<String, Workflow> workflows = new ConcurrentHashMap<>();

    @Override
    public Optional<Workflow> findById(String id) {
        return Optional.ofNullable(workflows.get(id));
    }

    @Override
    public List<Workflow> findAll() {
        return new ArrayList<>(workflows.values());
    }

    @Override
    public List<Workflow> findByEnabled(boolean enabled) {
        return workflows.values().stream()
            .filter(w -> w.isEnabled() == enabled)
            .collect(Collectors.toList());
    }

    @Override
    public Workflow save(Workflow workflow) {
        workflows.put(workflow.getId(), workflow);
        return workflow;
    }

    @Override
    public void delete(String id) {
        workflows.remove(id);
    }
}
