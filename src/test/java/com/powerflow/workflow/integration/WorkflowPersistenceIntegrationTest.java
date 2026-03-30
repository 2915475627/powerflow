package com.powerflow.workflow.integration;

import com.powerflow.workflow.adapter.outbound.persistence.EntityMapper;
import com.powerflow.workflow.adapter.outbound.persistence.JpaWorkflowRepository;
import com.powerflow.workflow.adapter.outbound.persistence.JpaNodeExecutionRepository;
import com.powerflow.workflow.adapter.outbound.persistence.PostgresWorkflowRepository;
import com.powerflow.workflow.adapter.outbound.persistence.PostgresExecutionLogRepository;
import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.model.enums.NodeType;
import com.powerflow.workflow.domain.service.ContextManager;
import com.powerflow.workflow.domain.service.NodeExecutorService;
import com.powerflow.workflow.domain.service.WorkflowExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class WorkflowPersistenceIntegrationTest {

    @Autowired
    private JpaWorkflowRepository workflowRepository;

    @Autowired
    private JpaNodeExecutionRepository executionRepository;

    private WorkflowExecutor workflowExecutor;
    private EntityMapper mapper;
    private PostgresWorkflowRepository pgWorkflowRepo;

    @BeforeEach
    void setUp() {
        mapper = new EntityMapper();
        pgWorkflowRepo = new PostgresWorkflowRepository(workflowRepository, mapper);
        PostgresExecutionLogRepository pgExecRepo = new PostgresExecutionLogRepository(executionRepository, mapper);
        ContextManager contextManager = new ContextManager();
        NodeExecutorService nodeExecutor = new NodeExecutorService(null);
        workflowExecutor = new WorkflowExecutor(pgWorkflowRepo, pgExecRepo, contextManager, nodeExecutor);
    }

    @Test
    void should_save_and_retrieve_workflow() {
        Node node = Node.builder()
            .id("test-node")
            .name("Test Node")
            .type(NodeType.DATA_PROCESSING)
            .config(Map.of("outputKey", "result", "expression", "#input.value * 2"))
            .inputMapping(Map.of("value", "input.value"))
            .outputMapping(Map.of("result", "output.result"))
            .build();

        Workflow workflow = Workflow.builder()
            .id("test-wf-" + System.currentTimeMillis())
            .name("Test Workflow")
            .startNodeId("test-node")
            .nodes(List.of(node))
            .edges(List.of())
            .build();

        pgWorkflowRepo.save(workflow);

        var saved = pgWorkflowRepo.findById(workflow.getId());
        assertThat(saved).isPresent();
        assertThat(saved.get().getName()).isEqualTo("Test Workflow");
    }

    @Test
    void should_execute_workflow_with_postgres_persistence() {
        Node node = Node.builder()
            .id("calc-node")
            .name("Calculate")
            .type(NodeType.DATA_PROCESSING)
            .config(Map.of("outputKey", "result", "expression", "#input.amount * 0.9"))
            .inputMapping(Map.of("amount", "input.amount"))
            .outputMapping(Map.of("result", "output.final"))
            .build();

        Workflow workflow = Workflow.builder()
            .id("exec-wf-" + System.currentTimeMillis())
            .name("Execution Test")
            .startNodeId("calc-node")
            .nodes(List.of(node))
            .edges(List.of())
            .build();

        pgWorkflowRepo.save(workflow);

        Context inputContext = new Context(Map.of("amount", 1000));
        WorkflowExecutionResult result = workflowExecutor.execute(workflow.getId(), inputContext);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFinalContext().get("output.final")).isPresent();
    }
}
