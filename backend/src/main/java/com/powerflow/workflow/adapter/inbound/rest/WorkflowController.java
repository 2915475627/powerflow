package com.powerflow.workflow.adapter.inbound.rest;

import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.port.inbound.WorkflowUseCase;
import com.powerflow.workflow.domain.port.outbound.TriggerExecutionLogRepository;
import com.powerflow.workflow.domain.port.outbound.WorkflowRepository;
import com.powerflow.workflow.domain.service.ContextManager;
import com.powerflow.workflow.domain.service.NodeExecutorService;
import com.powerflow.workflow.domain.service.WorkflowExecutor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController implements WorkflowUseCase {

    private final WorkflowExecutor workflowExecutor;
    private final WorkflowRepository workflowRepository;
    private final ContextManager contextManager;
    private final NodeExecutorService nodeExecutorService;
    private final TriggerExecutionLogRepository triggerLogRepository;

    public WorkflowController(WorkflowExecutor workflowExecutor,
                               WorkflowRepository workflowRepository,
                               ContextManager contextManager,
                               NodeExecutorService nodeExecutorService,
                               TriggerExecutionLogRepository triggerLogRepository) {
        this.workflowExecutor = workflowExecutor;
        this.workflowRepository = workflowRepository;
        this.contextManager = contextManager;
        this.nodeExecutorService = nodeExecutorService;
        this.triggerLogRepository = triggerLogRepository;
    }

    @Override
    @PostMapping("/{workflowId}/execute")
    public WorkflowExecutionResult execute(@PathVariable String workflowId, @RequestBody Context inputContext) {
        return workflowExecutor.execute(workflowId, inputContext);
    }

    @Override
    @PostMapping("/{workflowId}/nodes/{nodeId}/test")
    public NodeResult testNode(@PathVariable String workflowId,
                               @PathVariable String nodeId,
                               @RequestBody Context inputContext) {
        return workflowRepository.findById(workflowId)
            .flatMap(wf -> wf.findNodeById(nodeId))
            .map(node -> nodeExecutorService.execute(node, inputContext))
            .orElseThrow(() -> new RuntimeException("Node not found: " + nodeId));
    }

    @PostMapping
    public Workflow createWorkflow(@RequestBody Workflow workflow) {
        return workflowRepository.save(workflow);
    }

    @GetMapping("/{workflowId}")
    public Workflow getWorkflow(@PathVariable String workflowId) {
        return workflowRepository.findById(workflowId)
            .orElseThrow(() -> new RuntimeException("Workflow not found: " + workflowId));
    }

    @GetMapping
    public List<Workflow> listWorkflows() {
        return workflowRepository.findAll();
    }

    @GetMapping("/trigger-logs")
    public List<TriggerExecutionLog> getTriggerLogs(
            @RequestParam(required = false) String workflowId,
            @RequestParam(defaultValue = "50") int limit) {
        if (workflowId != null && !workflowId.isEmpty()) {
            return triggerLogRepository.findByWorkflowId(workflowId, limit);
        }
        return triggerLogRepository.findAll(limit);
    }
}
