package com.powerflow.workflow.integration;

import com.powerflow.workflow.adapter.outbound.persistence.InMemoryWorkflowRepository;
import com.powerflow.workflow.adapter.outbound.persistence.InMemoryTriggerExecutionLogRepository;
import com.powerflow.workflow.domain.model.Context;
import com.powerflow.workflow.domain.model.Node;
import com.powerflow.workflow.domain.model.Workflow;
import com.powerflow.workflow.domain.model.enums.NodeType;
import com.powerflow.workflow.domain.model.enums.TriggerType;
import com.powerflow.workflow.domain.port.outbound.TriggerExecutionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TriggerExecutionTest {

    private InMemoryWorkflowRepository workflowRepository;
    private InMemoryTriggerExecutionLogRepository triggerLogRepository;

    @BeforeEach
    void setUp() {
        workflowRepository = new InMemoryWorkflowRepository();
        triggerLogRepository = new InMemoryTriggerExecutionLogRepository();
    }

    @Test
    void testSaveAndFindTriggerLogs() {
        var log = com.powerflow.workflow.domain.model.TriggerExecutionLog.builder()
            .id("log-1")
            .workflowId("wf-1")
            .triggerType(TriggerType.SCHEDULE)
            .triggerSource("0 9 * * *")
            .status(com.powerflow.workflow.domain.model.enums.ExecutionStatus.SUCCESS)
            .executionId("exec-1")
            .triggeredAt(java.time.LocalDateTime.now())
            .build();

        triggerLogRepository.save(log);

        var logs = triggerLogRepository.findByWorkflowId("wf-1", 10);
        assertEquals(1, logs.size());
        assertEquals("log-1", logs.get(0).getId());
    }

    @Test
    void testFindByEnabled() {
        var workflow1 = createWorkflowWithStart("wf-1", true);
        var workflow2 = createWorkflowWithStart("wf-2", false);
        workflowRepository.save(workflow1);
        workflowRepository.save(workflow2);

        var enabled = workflowRepository.findByEnabled(true);
        var disabled = workflowRepository.findByEnabled(false);

        assertEquals(1, enabled.size());
        assertEquals(1, disabled.size());
        assertEquals("wf-1", enabled.get(0).getId());
    }

    @Test
    void testScheduleWorkflowHasStartNode() {
        var workflow = createScheduleWorkflow();
        workflowRepository.save(workflow);

        var startNode = workflow.findNodeById(workflow.getStartNodeId());
        assertTrue(startNode.isPresent());
        assertEquals(NodeType.START, startNode.get().getType());
        assertEquals("SCHEDULE", startNode.get().getConfig().get("triggerType"));
        assertEquals("0 9 * * *", startNode.get().getConfig().get("cron"));
    }

    @Test
    void testWebhookWorkflowHasStartNode() {
        var workflow = createWebhookWorkflow();
        workflowRepository.save(workflow);

        var startNode = workflow.findNodeById(workflow.getStartNodeId());
        assertTrue(startNode.isPresent());
        assertEquals(NodeType.START, startNode.get().getType());
        assertEquals("WEBHOOK", startNode.get().getConfig().get("triggerType"));
        assertEquals("/webhook/order", startNode.get().getConfig().get("webhookPath"));
    }

    private Workflow createWorkflowWithStart(String id, boolean enabled) {
        Node startNode = Node.builder()
            .id("start")
            .name("开始")
            .type(NodeType.START)
            .config(Map.of(
                "triggerType", "SCHEDULE",
                "cron", "0 9 * * *",
                "nextNodeId", "end"
            ))
            .inputMapping(Map.of())
            .outputMapping(Map.of())
            .build();

        Node endNode = Node.builder()
            .id("end")
            .name("结束")
            .type(NodeType.DATA_PROCESSING)
            .config(Map.of(
                "outputKey", "done",
                "expression", "'完成'"
            ))
            .inputMapping(Map.of())
            .outputMapping(Map.of())
            .build();

        return Workflow.builder()
            .id(id)
            .name("测试工作流")
            .description("测试")
            .startNodeId("start")
            .nodes(List.of(startNode, endNode))
            .edges(List.of())
            .enabled(enabled)
            .build();
    }

    private Workflow createScheduleWorkflow() {
        Node startNode = Node.builder()
            .id("start")
            .name("开始")
            .type(NodeType.START)
            .config(Map.of(
                "triggerType", "SCHEDULE",
                "cron", "0 9 * * *",
                "nextNodeId", "end"
            ))
            .inputMapping(Map.of())
            .outputMapping(Map.of())
            .build();

        Node endNode = Node.builder()
            .id("end")
            .name("结束")
            .type(NodeType.DATA_PROCESSING)
            .config(Map.of(
                "outputKey", "done",
                "expression", "'完成'"
            ))
            .inputMapping(Map.of())
            .outputMapping(Map.of())
            .build();

        return Workflow.builder()
            .id("schedule-wf")
            .name("定时工作流")
            .description("定时触发测试")
            .startNodeId("start")
            .nodes(List.of(startNode, endNode))
            .edges(List.of())
            .enabled(true)
            .build();
    }

    private Workflow createWebhookWorkflow() {
        Node startNode = Node.builder()
            .id("start")
            .name("开始")
            .type(NodeType.START)
            .config(Map.of(
                "triggerType", "WEBHOOK",
                "webhookPath", "/webhook/order",
                "fieldMappings", Map.of("orderId", "input.orderId"),
                "nextNodeId", "end"
            ))
            .inputMapping(Map.of())
            .outputMapping(Map.of())
            .build();

        Node endNode = Node.builder()
            .id("end")
            .name("结束")
            .type(NodeType.DATA_PROCESSING)
            .config(Map.of(
                "outputKey", "done",
                "expression", "'完成'"
            ))
            .inputMapping(Map.of())
            .outputMapping(Map.of())
            .build();

        return Workflow.builder()
            .id("webhook-wf")
            .name("Webhook工作流")
            .description("Webhook触发测试")
            .startNodeId("start")
            .nodes(List.of(startNode, endNode))
            .edges(List.of())
            .enabled(true)
            .build();
    }
}
