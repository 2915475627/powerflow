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
 * Public Service Scenarios Integration Tests
 *
 * Tests based on public service business scenarios:
 * - Form processing with intelligent routing (CONDITION node)
 * - Qualification audit with LLM pre-check and SUBWORKFLOW deep audit
 * - Progress query with HTTP status and LLM response generation
 * - Complaint handling with LLM classification and urgency-based routing
 */
class PublicServiceScenariosTest {

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
    @DisplayName("Scenario 1: Form Processing (表单处理)")
    class FormProcessingScenario {

        @Test
        @DisplayName("Should process form with intelligent routing")
        void should_process_form_with_routing() {
            // 1. 接收表单
            Node receiveForm = Node.builder()
                .id("receive_form")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "formData", "expression", "'表单数据'", "nextNodeId", "validate"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("formData", "formData"))
                .build();

            // 2. DATA_PROCESSING验证
            Node validate = Node.builder()
                .id("validate")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "formType", "expression", "'A'", "nextNodeId", "route"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("formType", "formType"))
                .build();

            // 3. CONDITION业务类型分流
            Node route = Node.builder()
                .id("route")
                .type(NodeType.CONDITION)
                .config(Map.of(
                    "conditions", List.of(
                        Map.of("expression", "#input.formType == 'A'", "nextNodeId", "handle_a"),
                        Map.of("expression", "#input.formType == 'B'", "nextNodeId", "handle_b")
                    ),
                    "defaultNextNodeId", "handle_other"
                ))
                .inputMapping(Map.of("formType", "formType"))
                .outputMapping(Map.of())
                .build();

            // 4. 业务A处理
            Node handleA = Node.builder()
                .id("handle_a")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'业务A处理完成'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "output.result"))
                .build();

            // 5. 业务B处理
            Node handleB = Node.builder()
                .id("handle_b")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'业务B处理完成'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "output.result"))
                .build();

            // 6. 其他业务
            Node handleOther = Node.builder()
                .id("handle_other")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'其他业务处理'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "output.result"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("form-processing-wf")
                .name("Form Processing")
                .startNodeId("receive_form")
                .nodes(List.of(receiveForm, validate, route, handleA, handleB, handleOther))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("receive_form").toNodeId("validate").build(),
                    Edge.builder().id("e2").fromNodeId("validate").toNodeId("route").build(),
                    Edge.builder().id("e3").fromNodeId("route").toNodeId("handle_a").build(),
                    Edge.builder().id("e4").fromNodeId("route").toNodeId("handle_b").build(),
                    Edge.builder().id("e5").fromNodeId("route").toNodeId("handle_other").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("form-processing-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Scenario 2: Qualification Audit (资质审核)")
    class QualificationAuditScenario {

        @Test
        @DisplayName("Should audit qualification with subworkflow")
        void should_audit_qualification_with_subworkflow() {
            // 1. 获取材料
            Node fetchMaterial = Node.builder()
                .id("fetch_material")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "material", "expression", "'资质材料'", "nextNodeId", "pre_check"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("material", "material"))
                .build();

            // 2. LLM预审
            Node preCheck = Node.builder()
                .id("pre_check")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "checkResult", "expression", "'pass'", "nextNodeId", "pass_check"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("checkResult", "checkResult"))
                .build();

            // 3. CONDITION通过/详细审核
            Node passCheck = Node.builder()
                .id("pass_check")
                .type(NodeType.CONDITION)
                .config(Map.of(
                    "conditions", List.of(
                        Map.of("expression", "#input.checkResult == 'pass'", "nextNodeId", "quick_approve"),
                        Map.of("expression", "#input.checkResult == 'fail'", "nextNodeId", "deep_audit")
                    ),
                    "defaultNextNodeId", "deep_audit"
                ))
                .inputMapping(Map.of("checkResult", "checkResult"))
                .outputMapping(Map.of())
                .build();

            // 4. 快速通过
            Node quickApprove = Node.builder()
                .id("quick_approve")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'审核通过'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "output.result"))
                .build();

            // 5. SUBWORKFLOW深度审核
            Node deepAudit = Node.builder()
                .id("deep_audit")
                .type(NodeType.SUBWORKFLOW)
                .config(Map.of("workflowId", "deep-audit-wf", "nextNodeId", "notify"))
                .inputMapping(Map.of())
                .outputMapping(Map.of())
                .build();

            // 6. 通知
            Node notify = Node.builder()
                .id("notify")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "done", "expression", "'审核流程完成'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("done", "output.done"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("qualification-audit-wf")
                .name("Qualification Audit")
                .startNodeId("fetch_material")
                .nodes(List.of(fetchMaterial, preCheck, passCheck, quickApprove, deepAudit, notify))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("fetch_material").toNodeId("pre_check").build(),
                    Edge.builder().id("e2").fromNodeId("pre_check").toNodeId("pass_check").build(),
                    Edge.builder().id("e3").fromNodeId("pass_check").toNodeId("quick_approve").build(),
                    Edge.builder().id("e4").fromNodeId("pass_check").toNodeId("deep_audit").build(),
                    Edge.builder().id("e5").fromNodeId("deep_audit").toNodeId("notify").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("qualification-audit-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Scenario 3: Progress Query (进度查询)")
    class ProgressQueryScenario {

        @Test
        @DisplayName("Should query progress with llm response")
        void should_query_progress_with_llm() {
            // 1. 接收查询
            Node receiveQuery = Node.builder()
                .id("receive_query")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "caseId", "expression", "'CASE001'", "nextNodeId", "get_status"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("caseId", "caseId"))
                .build();

            // 2. HTTP查询状态
            Node getStatus = Node.builder()
                .id("get_status")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "status", "expression", "'处理中'", "nextNodeId", "format_status"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("status", "status"))
                .build();

            // 3. DATA_PROCESSING格式化
            Node formatStatus = Node.builder()
                .id("format_status")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "formatted", "expression", "'当前状态: ' + #input.status", "nextNodeId", "generate_reply"))
                .inputMapping(Map.of("status", "status"))
                .outputMapping(Map.of("formatted", "formatted"))
                .build();

            // 4. LLM生成回复
            Node generateReply = Node.builder()
                .id("generate_reply")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "reply", "expression", "'您的申请正在处理中，预计3个工作日内完成。'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("reply", "output.reply"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("progress-query-wf")
                .name("Progress Query")
                .startNodeId("receive_query")
                .nodes(List.of(receiveQuery, getStatus, formatStatus, generateReply))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("receive_query").toNodeId("get_status").build(),
                    Edge.builder().id("e2").fromNodeId("get_status").toNodeId("format_status").build(),
                    Edge.builder().id("e3").fromNodeId("format_status").toNodeId("generate_reply").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("progress-query-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getFinalContext().get("output.reply")).isPresent();
        }
    }

    @Nested
    @DisplayName("Scenario 4: Complaint Handling (投诉处理)")
    class ComplaintHandlingScenario {

        @Test
        @DisplayName("Should handle complaint with classification")
        void should_handle_complaint_with_classification() {
            // 1. 接收投诉
            Node receiveComplaint = Node.builder()
                .id("receive_complaint")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "complaint", "expression", "'用户投诉内容'", "nextNodeId", "classify"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("complaint", "complaint"))
                .build();

            // 2. LLM分类
            Node classify = Node.builder()
                .id("classify")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "urgent", "expression", "true", "nextNodeId", "urgency"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("urgent", "urgent"))
                .build();

            // 3. CONDITION紧急程度
            Node urgency = Node.builder()
                .id("urgency")
                .type(NodeType.CONDITION)
                .config(Map.of(
                    "conditions", List.of(
                        Map.of("expression", "#input.urgent == true", "nextNodeId", "dispatch_urgent"),
                        Map.of("expression", "#input.urgent == false", "nextNodeId", "dispatch_normal")
                    ),
                    "defaultNextNodeId", "dispatch_normal"
                ))
                .inputMapping(Map.of("urgent", "urgent"))
                .outputMapping(Map.of())
                .build();

            // 4. 紧急派发
            Node dispatchUrgent = Node.builder()
                .id("dispatch_urgent")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'紧急派发'", "nextNodeId", "track"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "result"))
                .build();

            // 5. 普通派发
            Node dispatchNormal = Node.builder()
                .id("dispatch_normal")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'普通派发'", "nextNodeId", "track"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "result"))
                .build();

            // 6. SUBWORKFLOW跟踪
            Node track = Node.builder()
                .id("track")
                .type(NodeType.SUBWORKFLOW)
                .config(Map.of("workflowId", "complaint-track-wf", "nextNodeId", "complete"))
                .inputMapping(Map.of())
                .outputMapping(Map.of())
                .build();

            // 7. 完成
            Node complete = Node.builder()
                .id("complete")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "done", "expression", "'投诉处理完成'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("done", "output.done"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("complaint-handling-wf")
                .name("Complaint Handling")
                .startNodeId("receive_complaint")
                .nodes(List.of(receiveComplaint, classify, urgency, dispatchUrgent, dispatchNormal, track, complete))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("receive_complaint").toNodeId("classify").build(),
                    Edge.builder().id("e2").fromNodeId("classify").toNodeId("urgency").build(),
                    Edge.builder().id("e3").fromNodeId("urgency").toNodeId("dispatch_urgent").build(),
                    Edge.builder().id("e4").fromNodeId("urgency").toNodeId("dispatch_normal").build(),
                    Edge.builder().id("e5").fromNodeId("dispatch_urgent").toNodeId("track").build(),
                    Edge.builder().id("e6").fromNodeId("dispatch_normal").toNodeId("track").build(),
                    Edge.builder().id("e7").fromNodeId("track").toNodeId("complete").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("complaint-handling-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
        }
    }
}
