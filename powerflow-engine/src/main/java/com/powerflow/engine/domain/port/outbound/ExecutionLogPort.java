package com.powerflow.engine.domain.port.outbound;

import com.powerflow.engine.domain.model.NodeExecution;
import com.powerflow.engine.domain.model.WorkflowExecutionResult;

import java.util.List;

/**
 * Outbound port for execution log persistence.
 */
public interface ExecutionLogPort {
    void save(NodeExecution execution);
    List<NodeExecution> findByWorkflowExecutionId(String workflowExecutionId);
    void saveExecutionResult(WorkflowExecutionResult result);
    List<WorkflowExecutionResult> findByWorkflowId(String workflowId);
}
