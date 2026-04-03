package com.powerflow.engine.domain.port.inbound;

import com.powerflow.engine.domain.model.Context;
import com.powerflow.engine.domain.model.WorkflowExecutionResult;

/**
 * Main entry point for workflow execution.
 * This is the inbound port - external callers use this to execute workflows.
 */
public interface WorkflowEnginePort {
    /**
     * Execute a workflow by its ID.
     * @param workflowId the workflow ID
     * @param inputContext initial context data
     * @return execution result
     */
    WorkflowExecutionResult execute(String workflowId, Context inputContext);

    /**
     * Execute a workflow directly from a Workflow object.
     * @param workflow the workflow object
     * @param inputContext initial context data
     * @return execution result
     */
    WorkflowExecutionResult executeWorkflow(Object workflow, Context inputContext);
}
