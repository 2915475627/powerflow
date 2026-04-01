package com.powerflow.workflow.adapter.inbound.rest;

import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.port.inbound.WorkflowUseCase;
import com.powerflow.workflow.domain.port.outbound.TriggerExecutionLogRepository;
import com.powerflow.workflow.domain.port.outbound.WorkflowRepository;
import com.powerflow.workflow.domain.service.ContextManager;
import com.powerflow.workflow.domain.service.NodeExecutorService;
import com.powerflow.workflow.domain.service.WorkflowExecutor;
import com.powerflow.workflow.domain.service.validation.WorkflowValidationChain;
import com.powerflow.workflow.domain.service.validation.WorkflowValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController implements WorkflowUseCase {

    private final WorkflowExecutor workflowExecutor;
    private final WorkflowRepository workflowRepository;
    private final ContextManager contextManager;
    private final NodeExecutorService nodeExecutorService;
    private final TriggerExecutionLogRepository triggerLogRepository;
    private final WorkflowValidationChain validationChain;

    public WorkflowController(WorkflowExecutor workflowExecutor,
                               WorkflowRepository workflowRepository,
                               ContextManager contextManager,
                               NodeExecutorService nodeExecutorService,
                               TriggerExecutionLogRepository triggerLogRepository,
                               WorkflowValidationChain validationChain) {
        this.workflowExecutor = workflowExecutor;
        this.workflowRepository = workflowRepository;
        this.contextManager = contextManager;
        this.nodeExecutorService = nodeExecutorService;
        this.triggerLogRepository = triggerLogRepository;
        this.validationChain = validationChain;
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
    public ResponseEntity<?> createWorkflow(@RequestBody Workflow workflow) {
        System.out.println("DEBUG createWorkflow: workflow=" + workflow);
        System.out.println("DEBUG createWorkflow: nodes=" + workflow.getNodes());
        if (workflow.getNodes() != null) {
            workflow.getNodes().forEach((k, v) -> {
                System.out.println("DEBUG node key=" + k + " node=" + v + " type=" + (v != null ? v.getType() : "null"));
            });
        }
        validationChain.validate(workflow);
        Workflow saved = workflowRepository.save(workflow);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @ExceptionHandler(WorkflowValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(WorkflowValidationException ex) {
        List<Map<String, String>> errors = ex.getErrors().stream()
            .map(e -> Map.of("field", e.getField(), "message", e.getMessage()))
            .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("success", false, "errors", errors));
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
