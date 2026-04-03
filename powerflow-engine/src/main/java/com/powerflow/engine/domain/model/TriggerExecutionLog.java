package com.powerflow.engine.domain.model;

import com.powerflow.engine.domain.enums.ExecutionStatus;
import com.powerflow.engine.domain.enums.TriggerType;

import java.time.LocalDateTime;

public class TriggerExecutionLog {
    private String id;
    private String workflowId;
    private TriggerType triggerType;
    private String triggerSource;
    private ExecutionStatus status;
    private String executionId;
    private LocalDateTime triggeredAt;
    private String error;

    public TriggerExecutionLog(String id, String workflowId, TriggerType triggerType,
                               String triggerSource, ExecutionStatus status,
                               String executionId, LocalDateTime triggeredAt, String error) {
        this.id = id;
        this.workflowId = workflowId;
        this.triggerType = triggerType;
        this.triggerSource = triggerSource;
        this.status = status;
        this.executionId = executionId;
        this.triggeredAt = triggeredAt;
        this.error = error;
    }

    public String getId() { return id; }
    public String getWorkflowId() { return workflowId; }
    public TriggerType getTriggerType() { return triggerType; }
    public String getTriggerSource() { return triggerSource; }
    public ExecutionStatus getStatus() { return status; }
    public String getExecutionId() { return executionId; }
    public LocalDateTime getTriggeredAt() { return triggeredAt; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String workflowId;
        private TriggerType triggerType;
        private String triggerSource;
        private ExecutionStatus status;
        private String executionId;
        private LocalDateTime triggeredAt;
        private String error;

        public Builder id(String id) { this.id = id; return this; }
        public Builder workflowId(String workflowId) { this.workflowId = workflowId; return this; }
        public Builder triggerType(TriggerType triggerType) { this.triggerType = triggerType; return this; }
        public Builder triggerSource(String triggerSource) { this.triggerSource = triggerSource; return this; }
        public Builder status(ExecutionStatus status) { this.status = status; return this; }
        public Builder executionId(String executionId) { this.executionId = executionId; return this; }
        public Builder triggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public TriggerExecutionLog build() {
            return new TriggerExecutionLog(id, workflowId, triggerType, triggerSource,
                                         status, executionId, triggeredAt, error);
        }
    }
}
