package com.powerflow.workflow.domain.port.inbound;

import com.powerflow.workflow.domain.model.*;

public interface WorkflowUseCase {
    WorkflowExecutionResult execute(String workflowId, Context inputContext);
    NodeResult testNode(String workflowId, String nodeId, Context inputContext);
}
