package com.powerflow.workflow.adapter.outbound.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "node_executions")
public class NodeExecutionEntity {

    @Id
    private String id;

    private String workflowExecutionId;
    private String nodeId;
    private String status;

    @Convert(converter = MapToJsonConverter.class)
    private Map<String, Object> input = new HashMap<>();

    @Convert(converter = MapToJsonConverter.class)
    private Map<String, Object> output = new HashMap<>();

    private String error;
    private long durationMs;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkflowExecutionId() { return workflowExecutionId; }
    public void setWorkflowExecutionId(String workflowExecutionId) { this.workflowExecutionId = workflowExecutionId; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Map<String, Object> getInput() { return input; }
    public void setInput(Map<String, Object> input) { this.input = input; }
    public Map<String, Object> getOutput() { return output; }
    public void setOutput(Map<String, Object> output) { this.output = output; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
