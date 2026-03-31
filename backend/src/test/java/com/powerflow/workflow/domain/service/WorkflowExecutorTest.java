package com.powerflow.workflow.domain.service;

import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import com.powerflow.workflow.domain.model.enums.NodeType;
import com.powerflow.workflow.domain.port.outbound.ExecutionLogRepository;
import com.powerflow.workflow.domain.port.outbound.WorkflowRepository;
import com.powerflow.workflow.domain.service.handler.*;
import com.powerflow.workflow.adapter.outbound.http.RestTemplateHttpClientAdapter;
import com.powerflow.workflow.adapter.outbound.persistence.InMemoryWorkflowRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class WorkflowExecutorTest {

    @Test
    void should_execute_simple_linear_workflow() {
        WorkflowRepository repo = mock(WorkflowRepository.class);
        ExecutionLogRepository logRepo = mock(ExecutionLogRepository.class);

        HttpRequestHandler httpHandler = new HttpRequestHandler(new RestTemplateHttpClientAdapter());
        LlmCallHandler llmHandler = new LlmCallHandler();
        ParallelHandler parallelHandler = new ParallelHandler(new InMemoryWorkflowRepository());
        ForeachHandler foreachHandler = new ForeachHandler();
        SubworkflowHandler subworkflowHandler = new SubworkflowHandler(new InMemoryWorkflowRepository());
        TryCatchHandler tryCatchHandler = new TryCatchHandler();
        RetryHandler retryHandler = new RetryHandler();

        Node node1 = Node.builder()
            .id("node-1")
            .name("Start")
            .type(NodeType.DATA_PROCESSING)
            .config(Map.of("outputKey", "result", "expression", "#input.amount * 2"))
            .inputMapping(Map.of("amount", "input.amount"))
            .outputMapping(Map.of("result", "output.double"))
            .build();

        Workflow workflow = Workflow.builder()
            .id("wf-1")
            .name("Simple Workflow")
            .startNodeId("node-1")
            .nodes(List.of(node1))
            .edges(List.of())
            .build();

        when(repo.findById("wf-1")).thenReturn(Optional.of(workflow));

        ContextManager contextManager = new ContextManager();
        NodeExecutorService nodeExecutor = new NodeExecutorService(
            httpHandler, llmHandler, parallelHandler, foreachHandler,
            subworkflowHandler, tryCatchHandler, retryHandler
        );
        WorkflowExecutor executor = new WorkflowExecutor(repo, logRepo, contextManager, nodeExecutor);

        Context inputContext = new Context(Map.of("amount", 500));
        WorkflowExecutionResult result = executor.execute("wf-1", inputContext);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFinalContext().get("output.double")).contains(1000);
        verify(logRepo, atLeastOnce()).save(any(NodeExecution.class));
    }

    @Test
    void should_fail_when_workflow_not_found() {
        WorkflowRepository repo = mock(WorkflowRepository.class);
        ExecutionLogRepository logRepo = mock(ExecutionLogRepository.class);
        when(repo.findById("non-existent")).thenReturn(Optional.empty());

        HttpRequestHandler httpHandler = new HttpRequestHandler(new RestTemplateHttpClientAdapter());
        LlmCallHandler llmHandler = new LlmCallHandler();
        ParallelHandler parallelHandler = new ParallelHandler(new InMemoryWorkflowRepository());
        ForeachHandler foreachHandler = new ForeachHandler();
        SubworkflowHandler subworkflowHandler = new SubworkflowHandler(new InMemoryWorkflowRepository());
        TryCatchHandler tryCatchHandler = new TryCatchHandler();
        RetryHandler retryHandler = new RetryHandler();

        ContextManager contextManager = new ContextManager();
        NodeExecutorService nodeExecutor = new NodeExecutorService(
            httpHandler, llmHandler, parallelHandler, foreachHandler,
            subworkflowHandler, tryCatchHandler, retryHandler
        );
        WorkflowExecutor executor = new WorkflowExecutor(repo, logRepo, contextManager, nodeExecutor);

        Context ctx = new Context();
        assertThatThrownBy(() -> executor.execute("non-existent", ctx))
            .isInstanceOf(com.powerflow.workflow.exception.WorkflowNotFoundException.class);
    }
}
