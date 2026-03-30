package com.powerflow.workflow.adapter.outbound.persistence;

import com.powerflow.workflow.domain.model.Workflow;
import com.powerflow.workflow.domain.port.outbound.WorkflowRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "persistence.type", havingValue = "memory", matchIfMissing = true)
public class InMemoryWorkflowRepository implements WorkflowRepository {

    private final Map<String, Workflow> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Workflow> findById(String id) {
        return Optional.ofNullable(store.get(id));
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
