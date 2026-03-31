package com.powerflow.workflow.adapter.outbound.persistence;

import com.powerflow.workflow.domain.model.TriggerExecutionLog;
import com.powerflow.workflow.domain.port.outbound.TriggerExecutionLogRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class InMemoryTriggerExecutionLogRepository implements TriggerExecutionLogRepository {

    private final Map<String, List<TriggerExecutionLog>> logsByWorkflow = new ConcurrentHashMap<>();
    private final List<TriggerExecutionLog> allLogs = new ArrayList<>();

    @Override
    public void save(TriggerExecutionLog log) {
        logsByWorkflow.computeIfAbsent(log.getWorkflowId(), k -> new ArrayList<>()).add(log);
        allLogs.add(log);
    }

    @Override
    public List<TriggerExecutionLog> findByWorkflowId(String workflowId, int limit) {
        return logsByWorkflow.getOrDefault(workflowId, List.of()).stream()
            .sorted((a, b) -> b.getTriggeredAt().compareTo(a.getTriggeredAt()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public List<TriggerExecutionLog> findAll(int limit) {
        return allLogs.stream()
            .sorted((a, b) -> b.getTriggeredAt().compareTo(a.getTriggeredAt()))
            .limit(limit)
            .collect(Collectors.toList());
    }
}
