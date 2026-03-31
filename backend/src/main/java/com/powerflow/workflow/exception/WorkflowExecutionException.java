package com.powerflow.workflow.exception;

public class WorkflowExecutionException extends RuntimeException {
    private final String workflowId;
    private final String nodeId;

    public WorkflowExecutionException(String message, String workflowId, String nodeId) {
        super(message);
        this.workflowId = workflowId;
        this.nodeId = nodeId;
    }

    public String getWorkflowId() { return workflowId; }
    public String getNodeId() { return nodeId; }
}
