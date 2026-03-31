package com.powerflow.workflow.adapter.inbound.rest;

import com.powerflow.workflow.domain.model.WorkflowExecutionResult;
import com.powerflow.workflow.domain.port.outbound.ExecutionLogRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/executions")
public class ExecutionController {

    private final ExecutionLogRepository executionLogRepository;

    public ExecutionController(ExecutionLogRepository executionLogRepository) {
        this.executionLogRepository = executionLogRepository;
    }

    @GetMapping("/workflow/{workflowId}")
    public List<WorkflowExecutionResult> getByWorkflowId(@PathVariable String workflowId) {
        return executionLogRepository.findByWorkflowId(workflowId);
    }

    @GetMapping("/{executionId}")
    public WorkflowExecutionResult getByExecutionId(@PathVariable String executionId) {
        return executionLogRepository.findByWorkflowExecutionId(executionId)
            .stream()
            .findFirst()
            .map(ne -> WorkflowExecutionResult.builder()
                .executionId(ne.getWorkflowExecutionId())
                .workflowId(null) // NodeExecution doesn't store workflowId
                .status(ne.getStatus())
                .nodeExecutions(executionLogRepository.findByWorkflowExecutionId(executionId))
                .build())
            .orElseThrow(() -> new RuntimeException("Execution not found: " + executionId));
    }
}
