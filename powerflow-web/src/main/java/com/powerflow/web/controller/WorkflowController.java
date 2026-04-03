package com.powerflow.web.controller;

import com.powerflow.web.redis.RedisWorkflowLockAdapter;
import com.powerflow.engine.domain.model.Context;
import com.powerflow.engine.domain.model.Edge;
import com.powerflow.engine.domain.model.Node;
import com.powerflow.engine.domain.model.Workflow;
import com.powerflow.engine.domain.port.inbound.WorkflowEnginePort;
import com.powerflow.engine.domain.port.outbound.WorkflowRepositoryPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowRepositoryPort workflowRepository;
    private final WorkflowEnginePort workflowEngine;
    private final RedisWorkflowLockAdapter lockAdapter;

    public WorkflowController(
            WorkflowRepositoryPort workflowRepository,
            WorkflowEnginePort workflowEngine,
            RedisWorkflowLockAdapter lockAdapter) {
        this.workflowRepository = workflowRepository;
        this.workflowEngine = workflowEngine;
        this.lockAdapter = lockAdapter;
    }

    @PostMapping
    public ResponseEntity<Workflow> createWorkflow(@RequestBody CreateWorkflowRequest request) {
        // Convert nodes map to list for builder
        List<Node> nodesList = request.nodes() != null
            ? new java.util.ArrayList<>(request.nodes().values())
            : new java.util.ArrayList<>();

        Workflow workflow = Workflow.builder()
            .id(UUID.randomUUID().toString())
            .name(request.name())
            .description(request.description())
            .nodes(nodesList)
            .edges(request.edges())
            .startNodeId(request.startNodeId())
            .enabled(true)
            .build();

        Workflow saved = workflowRepository.save(workflow);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Workflow> getWorkflow(@PathVariable String id) {
        return workflowRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Workflow>> getAllWorkflows() {
        return ResponseEntity.ok(workflowRepository.findAll());
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, Object>> executeWorkflow(
            @PathVariable String id,
            @RequestBody Map<String, Object> input) {

        try {
            var result = workflowEngine.execute(id, new Context(input));
            return ResponseEntity.ok(Map.of(
                "status", result.getStatus(),
                "executionId", result.getExecutionId(),
                "output", result.getFinalContext() != null ? result.getFinalContext().toMap() : Map.of()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable String id) {
        if (workflowRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        workflowRepository.delete(id);
        return ResponseEntity.noContent().build();
    }

    public record CreateWorkflowRequest(
        String name,
        String description,
        Map<String, Node> nodes,
        List<Edge> edges,
        String startNodeId
    ) {}
}
