package com.powerflow.workflow.adapter.outbound.logging;

import com.powerflow.workflow.domain.model.NodeExecution;
import com.powerflow.workflow.domain.port.outbound.ExecutionLogRepository;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryExecutionLogRepository implements ExecutionLogRepository {

    private final Map<String, List<NodeExecution>> store = new ConcurrentHashMap<>();

    @Override
    public void save(NodeExecution execution) {
        store.computeIfAbsent(execution.getWorkflowExecutionId(), k -> new ArrayList<>())
             .add(execution);
    }

    @Override
    public List<NodeExecution> findByWorkflowExecutionId(String workflowExecutionId) {
        return new ArrayList<>(store.getOrDefault(workflowExecutionId, List.of()));
    }
}
