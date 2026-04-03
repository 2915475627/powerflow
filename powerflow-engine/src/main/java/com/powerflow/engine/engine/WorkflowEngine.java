package com.powerflow.engine.engine;

import com.powerflow.engine.domain.enums.ExecutionStatus;
import com.powerflow.engine.domain.model.*;
import com.powerflow.engine.domain.port.inbound.WorkflowEnginePort;
import com.powerflow.engine.domain.port.outbound.ExecutionLogPort;
import com.powerflow.engine.domain.port.outbound.WorkflowRepositoryPort;
import com.powerflow.engine.exception.WorkflowExecutionException;
import com.powerflow.engine.exception.WorkflowValidationException;
import com.powerflow.engine.validation.ValidationChain;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Main workflow execution engine.
 * Coordinates DAG building, validation, and node execution.
 */
public class WorkflowEngine implements WorkflowEnginePort {

    private final WorkflowRepositoryPort workflowRepository;
    private final ExecutionLogPort executionLog;
    private final NodeDispatcher nodeDispatcher;
    private final ContextManager contextManager;
    private final ValidationChain validationChain;

    public WorkflowEngine(WorkflowRepositoryPort workflowRepository,
                         ExecutionLogPort executionLog,
                         NodeDispatcher nodeDispatcher,
                         ContextManager contextManager,
                         ValidationChain validationChain) {
        this.workflowRepository = workflowRepository;
        this.executionLog = executionLog;
        this.nodeDispatcher = nodeDispatcher;
        this.contextManager = contextManager;
        this.validationChain = validationChain;
    }

    @Override
    public WorkflowExecutionResult execute(String workflowId, Context inputContext) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowExecutionException("Workflow not found: " + workflowId, workflowId, null));

        return executeWorkflow(workflow, inputContext);
    }

    @Override
    public WorkflowExecutionResult executeWorkflow(Object workflowObj, Context inputContext) {
        if (!(workflowObj instanceof Workflow workflow)) {
            throw new IllegalArgumentException("workflow must be a Workflow instance");
        }

        // Validate the workflow
        validationChain.validate(workflow);

        return executeInternal(workflow, new Context(inputContext.toMap()));
    }

    private WorkflowExecutionResult executeInternal(Workflow workflow, Context inputContext) {
        String executionId = UUID.randomUUID().toString();
        Context currentContext = inputContext;
        List<NodeExecution> executions = new ArrayList<>();
        String currentNodeId = workflow.getStartNodeId();

        while (currentNodeId != null) {
            Optional<Node> nodeOpt = workflow.findNodeById(currentNodeId);
            if (nodeOpt.isEmpty()) {
                break;
            }
            Node node = nodeOpt.get();

            LocalDateTime startTime = LocalDateTime.now();

            // Extract node input from context
            Map<String, Object> nodeInput = contextManager.extractNodeInput(node.getInputMapping(), currentContext);

            // Execute the node
            NodeResult result = nodeDispatcher.dispatch(node, currentContext);

            long durationMs = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();

            // Build and save execution record
            NodeExecution execution = NodeExecution.builder()
                .workflowExecutionId(executionId)
                .nodeId(node.getId())
                .status(result.getStatus())
                .input(nodeInput)
                .output(result.getOutput())
                .error(result.getError())
                .durationMs(durationMs)
                .startTime(startTime)
                .endTime(LocalDateTime.now())
                .build();

            executionLog.save(execution);
            executions.add(execution);

            if (!result.isSuccess()) {
                WorkflowExecutionResult failedResult = WorkflowExecutionResult.builder()
                    .workflowId(workflow.getId())
                    .executionId(executionId)
                    .status(ExecutionStatus.FAILED)
                    .finalContext(currentContext)
                    .nodeExecutions(executions)
                    .error("Node " + node.getId() + " failed: " + result.getError())
                    .build();
                executionLog.saveExecutionResult(failedResult);
                return failedResult;
            }

            // Write node output back to context
            contextManager.writeNodeOutput(node.getOutputMapping(), result.getOutput(), currentContext);

            // Determine next node
            String nextNodeId = result.getNextNodeId().get("nextNodeId");

            // For START nodes, use outgoing edge if nextNodeId is not set
            if (nextNodeId == null && node.getType() == com.powerflow.engine.domain.enums.NodeType.START) {
                nextNodeId = workflow.getEdges().stream()
                    .filter(e -> e.getFromNodeId().equals(node.getId()))
                    .findFirst()
                    .map(com.powerflow.engine.domain.model.Edge::getToNodeId)
                    .orElse(null);
            }

            currentNodeId = nextNodeId;
        }

        WorkflowExecutionResult successResult = WorkflowExecutionResult.builder()
            .workflowId(workflow.getId())
            .executionId(executionId)
            .status(ExecutionStatus.SUCCESS)
            .finalContext(currentContext)
            .nodeExecutions(executions)
            .build();
        executionLog.saveExecutionResult(successResult);
        return successResult;
    }
}
