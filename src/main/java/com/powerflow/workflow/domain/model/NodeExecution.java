package com.powerflow.workflow.domain.model;

import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class NodeExecution {
    private final String id;
    private final String workflowExecutionId;
    private final String nodeId;
    private final ExecutionStatus status;
    private final Map<String, Object> input;
    private final Map<String, Object> output;
    private final String error;
    private final long durationMs;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    private NodeExecution(String id, String workflowExecutionId, String nodeId,
                          ExecutionStatus status, Map<String, Object> input,
                          Map<String, Object> output, String error,
                          long durationMs, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.workflowExecutionId = workflowExecutionId;
        this.nodeId = nodeId;
        this.status = status;
        this.input = input;
        this.output = output;
        this.error = error;
        this.durationMs = durationMs;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static Builder builder() { return new Builder(); }

    public String getId() { return id; }
    public String getWorkflowExecutionId() { return workflowExecutionId; }
    public String getNodeId() { return nodeId; }
    public ExecutionStatus getStatus() { return status; }
    public Map<String, Object> getInput() { return input; }
    public Map<String, Object> getOutput() { return output; }
    public Optional<String> getError() { return Optional.ofNullable(error); }
    public long getDurationMs() { return durationMs; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }

    public static class Builder {
        private String id = UUID.randomUUID().toString();
        private String workflowExecutionId;
        private String nodeId;
        private ExecutionStatus status;
        private Map<String, Object> input;
        private Map<String, Object> output;
        private String error;
        private long durationMs;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        public Builder id(String id) { this.id = id; return this; }
        public Builder workflowExecutionId(String workflowExecutionId) { this.workflowExecutionId = workflowExecutionId; return this; }
        public Builder nodeId(String nodeId) { this.nodeId = nodeId; return this; }
        public Builder status(ExecutionStatus status) { this.status = status; return this; }
        public Builder input(Map<String, Object> input) { this.input = input; return this; }
        public Builder output(Map<String, Object> output) { this.output = output; return this; }
        public Builder error(String error) { this.error = error; return this; }
        public Builder durationMs(long durationMs) { this.durationMs = durationMs; return this; }
        public Builder startTime(LocalDateTime startTime) { this.startTime = startTime; return this; }
        public Builder endTime(LocalDateTime endTime) { this.endTime = endTime; return this; }
        public NodeExecution build() { return new NodeExecution(id, workflowExecutionId, nodeId, status, input, output, error, durationMs, startTime, endTime); }
    }
}
