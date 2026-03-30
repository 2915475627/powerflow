package com.powerflow.workflow.domain.port.outbound;

import com.powerflow.workflow.domain.model.NodeExecution;
import java.util.List;

public interface ExecutionLogRepository {
    void save(NodeExecution execution);
    List<NodeExecution> findByWorkflowExecutionId(String workflowExecutionId);
}
