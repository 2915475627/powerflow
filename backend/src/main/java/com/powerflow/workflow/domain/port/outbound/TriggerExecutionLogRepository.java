package com.powerflow.workflow.domain.port.outbound;

import com.powerflow.workflow.domain.model.TriggerExecutionLog;
import java.util.List;

public interface TriggerExecutionLogRepository {
    void save(TriggerExecutionLog log);
    List<TriggerExecutionLog> findByWorkflowId(String workflowId, int limit);
    List<TriggerExecutionLog> findAll(int limit);
}
