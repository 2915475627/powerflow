package com.powerflow.workflow.integration;

import com.powerflow.workflow.adapter.outbound.logging.InMemoryExecutionLogRepository;
import com.powerflow.workflow.adapter.outbound.persistence.InMemoryWorkflowRepository;
import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.model.enums.NodeType;
import com.powerflow.workflow.domain.service.ContextManager;
import com.powerflow.workflow.domain.service.NodeExecutorService;
import com.powerflow.workflow.domain.service.WorkflowExecutor;
import com.powerflow.workflow.domain.service.handler.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enterprise Business Scenarios Integration Tests
 *
 * Tests based on the enterprise office business scenarios design document.
 * These tests verify the workflow engine can handle:
 * - Multi-level approval with SUBWORKFLOW
 * - Parallel data fetching with FOREACH processing
 * - Task dispatch with foreach loop
 * - Meeting scheduling with LLM + CONDITION + HTTP
 */
class EnterpriseBusinessScenariosTest {

    private InMemoryWorkflowRepository workflowRepository;
    private InMemoryExecutionLogRepository logRepository;
    private WorkflowExecutor workflowExecutor;

    @BeforeEach
    void setUp() {
        workflowRepository = new InMemoryWorkflowRepository();
        logRepository = new InMemoryExecutionLogRepository();
        ContextManager contextManager = new ContextManager();

        HttpRequestHandler httpHandler = new HttpRequestHandler(
            new com.powerflow.workflow.adapter.outbound.http.RestTemplateHttpClientAdapter()
        );
        LlmCallHandler llmHandler = new LlmCallHandler();
        ParallelHandler parallelHandler = new ParallelHandler(workflowRepository);
        ForeachHandler foreachHandler = new ForeachHandler();
        SubworkflowHandler subworkflowHandler = new SubworkflowHandler(workflowRepository);
        TryCatchHandler tryCatchHandler = new TryCatchHandler();
        RetryHandler retryHandler = new RetryHandler();

        NodeExecutorService nodeExecutor = new NodeExecutorService(
            httpHandler, llmHandler, parallelHandler, foreachHandler,
            subworkflowHandler, tryCatchHandler, retryHandler
        );
        workflowExecutor = new WorkflowExecutor(workflowRepository, logRepository, contextManager, nodeExecutor);
    }

    @Nested
    @DisplayName("Scenario 1: Multi-Level Approval (多级审批)")
    class ApprovalFlowScenario {

        @Test
        @DisplayName("Should handle multi-level approval with subworkflow")
        void should_handle_multi_level_approval() {
            // 1. 接收申请
            Node receive = Node.builder()
                .id("receive")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "request", "expression", "'申请数据'", "nextNodeId", "level1"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("request", "request"))
                .build();

            // 2. 一级审批
            Node level1 = Node.builder()
                .id("level1")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "level1Result", "expression", "'一级审批通过'", "nextNodeId", "level2"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("level1Result", "level1Result"))
                .build();

            // 3. SUBWORKFLOW二级审批
            Node level2 = Node.builder()
                .id("level2")
                .type(NodeType.SUBWORKFLOW)
                .config(Map.of("workflowId", "approval-level2-wf", "nextNodeId", "notify"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("level2Result", "level2Result"))
                .build();

            // 4. 通知
            Node notify = Node.builder()
                .id("notify")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "done", "expression", "'审批流程完成'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("done", "output.done"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("approval-wf")
                .name("Approval Workflow")
                .startNodeId("receive")
                .nodes(List.of(receive, level1, level2, notify))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("receive").toNodeId("level1").build(),
                    Edge.builder().id("e2").fromNodeId("level1").toNodeId("level2").build(),
                    Edge.builder().id("e3").fromNodeId("level2").toNodeId("notify").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("approval-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Scenario 2: Report Generation (数据报表)")
    class ReportGenerationScenario {

        @Test
        @DisplayName("Should generate report with parallel fetch and foreach")
        void should_generate_report_with_parallel_and_foreach() {
            // 1. PARALLEL并行采集
            Node parallelFetch = Node.builder()
                .id("parallel_fetch")
                .type(NodeType.PARALLEL)
                .config(Map.of(
                    "branches", List.of(
                        Map.of("name", "sales", "nodeIds", List.of("sales_node")),
                        Map.of("name", "inventory", "nodeIds", List.of("inventory_node")),
                        Map.of("name", "users", "nodeIds", List.of("users_node"))
                    ),
                    "strategy", "AND",
                    "nextNodeId", "foreach_process"
                ))
                .inputMapping(Map.of())
                .outputMapping(Map.of())
                .build();

            // 2. 三个采集节点
            Node salesNode = Node.builder()
                .id("sales_node")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "sales", "expression", "10000"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("sales", "sales"))
                .build();

            Node inventoryNode = Node.builder()
                .id("inventory_node")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "inventory", "expression", "5000"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("inventory", "inventory"))
                .build();

            Node usersNode = Node.builder()
                .id("users_node")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "users", "expression", "1000"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("users", "users"))
                .build();

            // 3. FOREACH遍历处理
            Node foreachProcess = Node.builder()
                .id("foreach_process")
                .type(NodeType.FOREACH)
                .config(Map.of(
                    "collection", "#input.items",
                    "variableName", "item",
                    "maxIterations", 100,
                    "nextNodeId", "aggregate"
                ))
                .inputMapping(Map.of("items", "input.items"))
                .outputMapping(Map.of("results", "processed"))
                .build();

            // 4. 汇总
            Node aggregate = Node.builder()
                .id("aggregate")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "report", "expression", "'报表生成完成'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("report", "output.report"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("report-wf")
                .name("Report Generation")
                .startNodeId("parallel_fetch")
                .nodes(List.of(parallelFetch, salesNode, inventoryNode, usersNode, foreachProcess, aggregate))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("parallel_fetch").toNodeId("sales_node").build(),
                    Edge.builder().id("e2").fromNodeId("parallel_fetch").toNodeId("inventory_node").build(),
                    Edge.builder().id("e3").fromNodeId("parallel_fetch").toNodeId("users_node").build(),
                    Edge.builder().id("e4").fromNodeId("sales_node").toNodeId("foreach_process").build(),
                    Edge.builder().id("e5").fromNodeId("inventory_node").toNodeId("foreach_process").build(),
                    Edge.builder().id("e6").fromNodeId("users_node").toNodeId("foreach_process").build(),
                    Edge.builder().id("e7").fromNodeId("foreach_process").toNodeId("aggregate").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of("items", List.of("item1", "item2", "item3")));
            WorkflowExecutionResult result = workflowExecutor.execute("report-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Scenario 3: Task Dispatch (任务分发)")
    class TaskDispatchScenario {

        @Test
        @DisplayName("Should dispatch tasks in foreach loop")
        void should_dispatch_tasks_in_foreach() {
            // 1. FOREACH分发
            Node foreachDispatch = Node.builder()
                .id("foreach_dispatch")
                .type(NodeType.FOREACH)
                .config(Map.of(
                    "collection", "#input.tasks",
                    "variableName", "task",
                    "maxIterations", 50,
                    "nextNodeId", "summary"
                ))
                .inputMapping(Map.of("tasks", "input.tasks"))
                .outputMapping(Map.of("results", "dispatchResults"))
                .build();

            // 2. 汇总
            Node summary = Node.builder()
                .id("summary")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "total", "expression", "#input.totalIterations"))
                .inputMapping(Map.of("totalIterations", "dispatchResults.totalIterations"))
                .outputMapping(Map.of("total", "output.total"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("task-dispatch-wf")
                .name("Task Dispatch")
                .startNodeId("foreach_dispatch")
                .nodes(List.of(foreachDispatch, summary))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("foreach_dispatch").toNodeId("summary").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of("tasks", List.of("任务A", "任务B", "任务C")));
            WorkflowExecutionResult result = workflowExecutor.execute("task-dispatch-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Scenario 4: Meeting Schedule (会议安排)")
    class MeetingScheduleScenario {

        @Test
        @DisplayName("Should schedule meeting with condition")
        void should_schedule_meeting_with_condition() {
            // 1. LLM解析会议请求
            Node parseRequest = Node.builder()
                .id("parse_request")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "meetingInfo", "expression", "'会议信息已解析'", "nextNodeId", "check_available"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("meetingInfo", "meetingInfo"))
                .build();

            // 2. CONDITION检查日程
            Node checkAvailable = Node.builder()
                .id("check_available")
                .type(NodeType.CONDITION)
                .config(Map.of(
                    "conditions", List.of(
                        Map.of("expression", "#input.available == true", "nextNodeId", "book_room"),
                        Map.of("expression", "#input.available == false", "nextNodeId", "notify_conflict")
                    ),
                    "defaultNextNodeId", "notify_conflict"
                ))
                .inputMapping(Map.of("available", "input.available"))
                .outputMapping(Map.of())
                .build();

            // 3. 预定会议室
            Node bookRoom = Node.builder()
                .id("book_room")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'会议室已预定'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "output.result"))
                .build();

            // 4. 通知冲突
            Node notifyConflict = Node.builder()
                .id("notify_conflict")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'时间冲突'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "output.result"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("meeting-schedule-wf")
                .name("Meeting Schedule")
                .startNodeId("parse_request")
                .nodes(List.of(parseRequest, checkAvailable, bookRoom, notifyConflict))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("parse_request").toNodeId("check_available").build(),
                    Edge.builder().id("e2").fromNodeId("check_available").toNodeId("book_room").build(),
                    Edge.builder().id("e3").fromNodeId("check_available").toNodeId("notify_conflict").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of("available", true));
            WorkflowExecutionResult result = workflowExecutor.execute("meeting-schedule-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getFinalContext().get("output.result").map(Object::toString).orElse(""))
                .isEqualTo("会议室已预定");
        }
    }
}
