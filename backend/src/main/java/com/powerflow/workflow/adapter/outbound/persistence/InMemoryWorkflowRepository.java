package com.powerflow.workflow.adapter.outbound.persistence;

import com.powerflow.workflow.domain.model.Workflow;
import com.powerflow.workflow.domain.port.outbound.WorkflowRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryWorkflowRepository implements WorkflowRepository {

    private final Map<String, Workflow> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Workflow> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Workflow> findAll() {
        return store.values().stream().collect(Collectors.toList());
    }

    @Override
    public List<Workflow> findByEnabled(boolean enabled) {
        return store.values().stream()
            .filter(w -> w.isEnabled() == enabled)
            .collect(Collectors.toList());
    }

    @Override
    public Workflow save(Workflow workflow) {
        store.put(workflow.getId(), workflow);
        return workflow;
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }
}
