package com.powerflow.workflow.domain.model;

import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import java.util.List;
import java.util.Optional;

public class WorkflowExecutionResult {
    private final String workflowId;
    private final String executionId;
    private final ExecutionStatus status;
    private final Context finalContext;
    private final List<NodeExecution> nodeExecutions;
    private final Optional<String> error;

    private WorkflowExecutionResult(String workflowId, String executionId, ExecutionStatus status,
                                     Context finalContext, List<NodeExecution> nodeExecutions,
                                     Optional<String> error) {
        this.workflowId = workflowId;
        this.executionId = executionId;
        this.status = status;
        this.finalContext = finalContext;
        this.nodeExecutions = nodeExecutions;
        this.error = error;
    }

    public static Builder builder() { return new Builder(); }

    public String getWorkflowId() { return workflowId; }
    public String getExecutionId() { return executionId; }
    public ExecutionStatus getStatus() { return status; }
    public Context getFinalContext() { return finalContext; }
    public List<NodeExecution> getNodeExecutions() { return nodeExecutions; }
    public Optional<String> getError() { return error; }
    public boolean isSuccess() { return status == ExecutionStatus.SUCCESS; }

    public static class Builder {
        private String workflowId;
        private String executionId;
        private ExecutionStatus status;
        private Context finalContext;
        private List<NodeExecution> nodeExecutions;
        private Optional<String> error = Optional.empty();

        public Builder workflowId(String workflowId) { this.workflowId = workflowId; return this; }
        public Builder executionId(String executionId) { this.executionId = executionId; return this; }
        public Builder status(ExecutionStatus status) { this.status = status; return this; }
        public Builder finalContext(Context finalContext) { this.finalContext = finalContext; return this; }
        public Builder nodeExecutions(List<NodeExecution> nodeExecutions) { this.nodeExecutions = nodeExecutions; return this; }
        public Builder error(String error) { this.error = Optional.of(error); return this; }
        public WorkflowExecutionResult build() { return new WorkflowExecutionResult(workflowId, executionId, status, finalContext, nodeExecutions, error); }
    }
}
