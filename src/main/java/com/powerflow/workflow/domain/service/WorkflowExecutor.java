package com.powerflow.workflow.domain.service;

import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import com.powerflow.workflow.domain.port.outbound.ExecutionLogRepository;
import com.powerflow.workflow.domain.port.outbound.WorkflowRepository;
import com.powerflow.workflow.exception.WorkflowNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class WorkflowExecutor {

    private final WorkflowRepository workflowRepository;
    private final ExecutionLogRepository executionLogRepository;
    private final ContextManager contextManager;
    private final NodeExecutorService nodeExecutor;

    public WorkflowExecutor(WorkflowRepository workflowRepository,
                            ExecutionLogRepository executionLogRepository,
                            ContextManager contextManager,
                            NodeExecutorService nodeExecutor) {
        this.workflowRepository = workflowRepository;
        this.executionLogRepository = executionLogRepository;
        this.contextManager = contextManager;
        this.nodeExecutor = nodeExecutor;
    }

    public WorkflowExecutionResult execute(String workflowId, Context inputContext) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));

        return executeWorkflow(workflow, inputContext);
    }

    private WorkflowExecutionResult executeWorkflow(Workflow workflow, Context inputContext) {
        String executionId = UUID.randomUUID().toString();
        Context currentContext = new Context(inputContext.toMap());
        List<NodeExecution> executions = new ArrayList<>();
        String currentNodeId = workflow.getStartNodeId();

        while (currentNodeId != null) {
            Optional<Node> nodeOpt = workflow.findNodeById(currentNodeId);
            if (nodeOpt.isEmpty()) {
                break;
            }
            Node node = nodeOpt.get();

            LocalDateTime startTime = LocalDateTime.now();

            Map<String, Object> nodeInput = contextManager.extractNodeInput(node.getInputMapping(), currentContext);
            NodeResult result = nodeExecutor.execute(node, currentContext);

            long durationMs = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();

            NodeExecution execution = NodeExecution.builder()
                .workflowExecutionId(executionId)
                .nodeId(node.getId())
                .status(result.getStatus())
                .input(nodeInput)
                .output(result.getOutput())
                .error(result.getError().orElse(null))
                .durationMs(durationMs)
                .startTime(startTime)
                .endTime(LocalDateTime.now())
                .build();

            executionLogRepository.save(execution);
            executions.add(execution);

            if (!result.isSuccess()) {
                return WorkflowExecutionResult.builder()
                    .workflowId(workflow.getId())
                    .executionId(executionId)
                    .status(ExecutionStatus.FAILED)
                    .finalContext(currentContext)
                    .nodeExecutions(executions)
                    .error("Node " + node.getId() + " failed: " + result.getError().orElse("Unknown error"))
                    .build();
            }

            contextManager.writeNodeOutput(node.getOutputMapping(), result.getOutput(), currentContext);

            // Get next node ID from result's nextNodeId map
            String nextNodeId = result.getNextNodeId().get("nextNodeId");
            currentNodeId = nextNodeId;
        }

        return WorkflowExecutionResult.builder()
            .workflowId(workflow.getId())
            .executionId(executionId)
            .status(ExecutionStatus.SUCCESS)
            .finalContext(currentContext)
            .nodeExecutions(executions)
            .build();
    }
}