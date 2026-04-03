package com.powerflow.web.persistence;

import com.powerflow.engine.domain.model.NodeExecution;
import com.powerflow.engine.domain.model.WorkflowExecutionResult;
import com.powerflow.engine.domain.port.outbound.ExecutionLogPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryExecutionLog implements ExecutionLogPort {

    private final Map<String, NodeExecution> executions = new ConcurrentHashMap<>();
    private final Map<String, WorkflowExecutionResult> results = new ConcurrentHashMap<>();

    @Override
    public void save(NodeExecution execution) {
        executions.put(execution.getNodeId() + "-" + execution.getStartTime(), execution);
    }

    @Override
    public List<NodeExecution> findByWorkflowExecutionId(String workflowExecutionId) {
        return executions.values().stream()
            .filter(e -> e.getWorkflowExecutionId().equals(workflowExecutionId))
            .collect(Collectors.toList());
    }

    @Override
    public void saveExecutionResult(WorkflowExecutionResult result) {
        results.put(result.getExecutionId(), result);
    }

    @Override
    public List<WorkflowExecutionResult> findByWorkflowId(String workflowId) {
        return results.values().stream()
            .filter(r -> r.getWorkflowId().equals(workflowId))
            .collect(Collectors.toList());
    }
}
