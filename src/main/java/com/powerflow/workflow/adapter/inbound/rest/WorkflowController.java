package com.powerflow.workflow.adapter.inbound.rest;

import com.powerflow.workflow.adapter.outbound.persistence.EntityMapper;
import com.powerflow.workflow.adapter.outbound.persistence.JpaNodeExecutionRepository;
import com.powerflow.workflow.adapter.outbound.persistence.PostgresExecutionLogRepository;
import com.powerflow.workflow.adapter.outbound.persistence.PostgresWorkflowRepository;
import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.port.inbound.WorkflowUseCase;
import com.powerflow.workflow.domain.port.outbound.WorkflowRepository;
import com.powerflow.workflow.domain.service.ContextManager;
import com.powerflow.workflow.domain.service.NodeExecutorService;
import com.powerflow.workflow.domain.service.WorkflowExecutor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController implements WorkflowUseCase {

    private final WorkflowExecutor workflowExecutor;
    private final WorkflowRepository workflowRepository;
    private final EntityMapper mapper;

    public WorkflowController(JpaNodeExecutionRepository executionRepository) {
        this.mapper = new EntityMapper();
        PostgresWorkflowRepository pgWorkflowRepo = new PostgresWorkflowRepository(null, mapper);
        PostgresExecutionLogRepository pgExecRepo = new PostgresExecutionLogRepository(executionRepository, mapper);
        ContextManager contextManager = new ContextManager();
        NodeExecutorService nodeExecutor = new NodeExecutorService(null);
        this.workflowExecutor = new WorkflowExecutor(pgWorkflowRepo, pgExecRepo, contextManager, nodeExecutor);
        this.workflowRepository = pgWorkflowRepo;
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
            .map(node -> {
                NodeExecutorService executor = new NodeExecutorService(null);
                return executor.execute(node, inputContext);
            })
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
}