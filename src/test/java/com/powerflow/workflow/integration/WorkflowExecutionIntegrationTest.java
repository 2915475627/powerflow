package com.powerflow.workflow.integration;

import com.powerflow.workflow.adapter.outbound.logging.InMemoryExecutionLogRepository;
import com.powerflow.workflow.adapter.outbound.persistence.InMemoryWorkflowRepository;
import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.model.enums.NodeType;
import com.powerflow.workflow.domain.service.ContextManager;
import com.powerflow.workflow.domain.service.NodeExecutorService;
import com.powerflow.workflow.domain.service.WorkflowExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class WorkflowExecutionIntegrationTest {

    private InMemoryWorkflowRepository workflowRepository;
    private InMemoryExecutionLogRepository logRepository;
    private WorkflowExecutor workflowExecutor;

    @BeforeEach
    void setUp() {
        workflowRepository = new InMemoryWorkflowRepository();
        logRepository = new InMemoryExecutionLogRepository();
        ContextManager contextManager = new ContextManager();
        NodeExecutorService nodeExecutor = new NodeExecutorService();
        workflowExecutor = new WorkflowExecutor(workflowRepository, logRepository, contextManager, nodeExecutor);
    }

    @Test
    void should_execute_full_workflow_with_multiple_nodes() {
        Node calculateDiscount = Node.builder()
            .id("discount")
            .name("Calculate Discount")
            .type(NodeType.DATA_PROCESSING)
            .config(Map.of("outputKey", "discountedAmount", "expression", "#input.amount * 0.9", "nextNodeId", "check"))
            .inputMapping(Map.of("amount", "input.amount"))
            .outputMapping(Map.of("discountedAmount", "output.discounted"))
            .build();

        Node checkDiscount = Node.builder()
            .id("check")
            .name("Check Discount")
            .type(NodeType.CONDITION)
            .config(Map.of(
                "conditions", List.of(
                    Map.of("expression", "#input.discountedAmount > 500", "nextNodeId", "high-discount"),
                    Map.of("expression", "#input.discountedAmount > 100", "nextNodeId", "medium-discount")
                ),
                "defaultNextNodeId", "low-discount"
            ))
            .inputMapping(Map.of("discountedAmount", "output.discounted"))
            .outputMapping(Map.of())
            .build();

        Workflow workflow = Workflow.builder()
            .id("test-wf")
            .name("Discount Workflow")
            .startNodeId("discount")
            .nodes(List.of(calculateDiscount, checkDiscount))
            .edges(List.of(Edge.builder().id("e1").fromNodeId("discount").toNodeId("check").build()))
            .build();

        workflowRepository.save(workflow);

        Context inputContext = new Context(Map.of("amount", 1000));
        WorkflowExecutionResult result = workflowExecutor.execute("test-wf", inputContext);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFinalContext().get("output.discounted")).hasValue(900.0);
        assertThat(result.getNodeExecutions()).hasSize(2);
    }
}