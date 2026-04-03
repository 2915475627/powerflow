package com.powerflow.example;

import com.powerflow.engine.domain.enums.NodeType;
import com.powerflow.engine.domain.model.Context;
import com.powerflow.engine.domain.model.Edge;
import com.powerflow.engine.domain.model.Node;
import com.powerflow.engine.domain.model.NodeResult;
import com.powerflow.engine.domain.model.Workflow;
import com.powerflow.engine.domain.model.WorkflowExecutionResult;
import com.powerflow.engine.engine.ContextManager;
import com.powerflow.engine.engine.NodeDispatcher;
import com.powerflow.engine.engine.WorkflowEngine;
import com.powerflow.engine.domain.port.inbound.WorkflowEnginePort;
import com.powerflow.engine.engine.WorkflowEngine;
import com.powerflow.engine.spi.NodeExecutorLoader;
import com.powerflow.engine.validation.ValidationChain;
import com.powerflow.engine.validation.CycleDetectionValidator;
import com.powerflow.engine.validation.StartNodeValidator;
import com.powerflow.engine.validation.WorkflowStructureValidator;
import com.powerflow.engine.validation.WorkflowValidator;

import java.util.*;

public class Demo {

    public static void main(String[] args) {
        System.out.println("=== PowerFlow Demo - Pure Java Workflow Engine ===\n");

        // Step 1: Set up node executors via SPI
        NodeDispatcher dispatcher = new NodeDispatcher();
        NodeExecutorLoader.loadNodesInto(dispatcher);

        System.out.println("Node executors registered successfully");

        // Step 2: Create in-memory port implementations
        InMemoryWorkflowRepository repository = new InMemoryWorkflowRepository();
        InMemoryExecutionLog executionLog = new InMemoryExecutionLog();
        ContextManager contextManager = new ContextManager();
        ValidationChain validationChain = createValidationChain();

        // Step 3: Create workflow engine
        WorkflowEnginePort engine = new WorkflowEngine(
            repository,
            executionLog,
            dispatcher,
            contextManager,
            validationChain
        );

        // Step 4: Create workflow programmatically
        String workflowId = UUID.randomUUID().toString();

        Node startNode = Node.builder()
            .id("start-1")
            .type(NodeType.START)
            .config(Map.of("nextNodeId", "process-1"))
            .build();

        Node dataProcessNode = Node.builder()
            .id("process-1")
            .type(NodeType.DATA_PROCESSING)
            .config(Map.of(
                "operation", "transform",
                "script", "input.put('processed', true); input.put('timestamp', System.currentTimeMillis());",
                "nextNodeId", "end-1"
            ))
            .build();

        Node endNode = Node.builder()
            .id("end-1")
            .type(NodeType.END)
            .config(Map.of())
            .build();

        List<Node> nodes = List.of(startNode, dataProcessNode, endNode);

        List<Edge> edges = List.of(
            Edge.builder().id("e1").fromNodeId("start-1").toNodeId("process-1").build(),
            Edge.builder().id("e2").fromNodeId("process-1").toNodeId("end-1").build()
        );

        Workflow workflow = Workflow.builder()
            .id(workflowId)
            .name("Demo Workflow")
            .description("Simple demo workflow")
            .nodes(nodes)
            .edges(edges)
            .startNodeId("start-1")
            .enabled(true)
            .build();

        // Step 5: Save workflow
        repository.save(workflow);
        System.out.println("Created workflow: " + workflow.getName() + " (ID: " + workflowId + ")");

        // Step 6: Create context with input
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("message", "Hello PowerFlow!");
        inputData.put("data", List.of(1, 2, 3, 4, 5));

        Context context = new Context(inputData);

        // Step 7: Execute workflow
        System.out.println("\nExecuting workflow: " + workflow.getName());
        System.out.println("Input: " + inputData);

        WorkflowExecutionResult result = engine.executeWorkflow(workflow, context);

        // Step 8: Output results
        System.out.println("\n=== Execution Result ===");
        System.out.println("Status: " + result.getStatus());
        System.out.println("Execution ID: " + result.getExecutionId());
        if (result.getFinalContext() != null) {
            System.out.println("Output: " + result.getFinalContext().toMap());
        }

        if (result.getError() != null) {
            System.out.println("Error: " + result.getError());
        }

        System.out.println("\n=== Demo Complete ===");
    }

    private static ValidationChain createValidationChain() {
        List<WorkflowValidator> validators = List.of(
            new StartNodeValidator(),
            new WorkflowStructureValidator(),
            new CycleDetectionValidator()
        );
        return workflow -> {
            for (WorkflowValidator v : validators) {
                v.validate(workflow);
            }
        };
    }

    // Simple in-memory implementations for demo
    static class InMemoryWorkflowRepository implements com.powerflow.engine.domain.port.outbound.WorkflowRepositoryPort {
        private final Map<String, Workflow> workflows = new HashMap<>();

        @Override
        public Optional<Workflow> findById(String id) {
            return Optional.ofNullable(workflows.get(id));
        }

        @Override
        public List<Workflow> findAll() {
            return new ArrayList<>(workflows.values());
        }

        @Override
        public List<Workflow> findByEnabled(boolean enabled) {
            return workflows.values().stream()
                .filter(w -> w.isEnabled() == enabled)
                .toList();
        }

        @Override
        public Workflow save(Workflow workflow) {
            workflows.put(workflow.getId(), workflow);
            return workflow;
        }

        @Override
        public void delete(String id) {
            workflows.remove(id);
        }
    }

    static class InMemoryExecutionLog implements com.powerflow.engine.domain.port.outbound.ExecutionLogPort {
        @Override
        public void save(com.powerflow.engine.domain.model.NodeExecution execution) {
            // No-op for demo
        }

        @Override
        public List<com.powerflow.engine.domain.model.NodeExecution> findByWorkflowExecutionId(String workflowExecutionId) {
            return List.of();
        }

        @Override
        public void saveExecutionResult(com.powerflow.engine.domain.model.WorkflowExecutionResult result) {
            // No-op for demo
        }

        @Override
        public List<com.powerflow.engine.domain.model.WorkflowExecutionResult> findByWorkflowId(String workflowId) {
            return List.of();
        }
    }
}
