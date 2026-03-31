package com.powerflow.workflow.exception;

public class NodeExecutionException extends RuntimeException {
    private final String nodeId;

    public NodeExecutionException(String message, String nodeId) {
        super(message);
        this.nodeId = nodeId;
    }

    public String getNodeId() { return nodeId; }
}
