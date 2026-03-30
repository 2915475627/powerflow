package com.powerflow.workflow.domain.model;

import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import java.util.Map;
import java.util.Optional;

public class NodeResult {
    private final String nodeId;
    private final ExecutionStatus status;
    private final Map<String, Object> output;
    private final Optional<String> error;
    private final Map<String, String> nextNodeId;

    private NodeResult(String nodeId, ExecutionStatus status, Map<String, Object> output,
                       Optional<String> error, Map<String, String> nextNodeId) {
        this.nodeId = nodeId;
        this.status = status;
        this.output = output;
        this.error = error;
        this.nextNodeId = nextNodeId;
    }

    public static Builder builder() { return new Builder(); }

    public String getNodeId() { return nodeId; }
    public ExecutionStatus getStatus() { return status; }
    public Map<String, Object> getOutput() { return output; }
    public Optional<String> getError() { return error; }
    public Map<String, String> getNextNodeId() { return nextNodeId; }

    public boolean isSuccess() { return status == ExecutionStatus.SUCCESS; }

    public static class Builder {
        private String nodeId;
        private ExecutionStatus status;
        private Map<String, Object> output;
        private Optional<String> error = Optional.empty();
        private Map<String, String> nextNodeId = Map.of();

        public Builder nodeId(String nodeId) { this.nodeId = nodeId; return this; }
        public Builder status(ExecutionStatus status) { this.status = status; return this; }
        public Builder output(Map<String, Object> output) { this.output = output; return this; }
        public Builder error(String error) { this.error = Optional.of(error); return this; }
        public Builder nextNodeId(String nextNodeId) { this.nextNodeId = Map.of("nextNodeId", nextNodeId); return this; }
        public NodeResult build() { return new NodeResult(nodeId, status, output, error, nextNodeId); }
    }
}
