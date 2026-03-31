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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Finance Business Scenarios Integration Tests
 *
 * Tests based on the finance MVP business scenarios design document.
 * These tests verify the workflow engine can handle:
 * - Risk assessment with HTTP fetch and LLM scoring
 * - Loan approval with try-catch and subworkflow
 * - Anti-fraud detection with parallel processing
 * - Account audit with nested subworkflow
 */
class FinanceBusinessScenariosTest {

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
    @DisplayName("Scenario 1: Risk Assessment (风控评估)")
    class RiskAssessmentScenario {

        @Test
        @DisplayName("Should assess risk and make decision")
        void should_assess_risk_and_decide() {
            // 1. 模拟HTTP获取征信 - DATA_PROCESSING模拟返回creditScore=750
            Node fetchCredit = Node.builder()
                .id("fetch_credit")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of(
                    "outputKey", "creditScore",
                    "expression", "750",
                    "nextNodeId", "risk_score"
                ))
                .inputMapping(Map.of())
                .outputMapping(Map.of("creditScore", "creditScore"))
                .build();

            // 2. DATA_PROCESSING模拟LLM评分
            Node riskScore = Node.builder()
                .id("risk_score")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of(
                    "outputKey", "riskLevel",
                    "expression", "0.85",
                    "nextNodeId", "decision"
                ))
                .inputMapping(Map.of())
                .outputMapping(Map.of("riskLevel", "riskLevel"))
                .build();

            // 3. CONDITION决策 - score>=0.7通过 / <0.7拒绝
            List<Map<String, String>> conditions = new ArrayList<>();
            conditions.add(Map.of("expression", "#input.riskLevel >= 0.7", "nextNodeId", "approve"));
            conditions.add(Map.of("expression", "#input.riskLevel < 0.7", "nextNodeId", "reject"));

            Node decision = Node.builder()
                .id("decision")
                .type(NodeType.CONDITION)
                .config(Map.of(
                    "conditions", conditions,
                    "defaultNextNodeId", "reject"
                ))
                .inputMapping(Map.of("riskLevel", "riskLevel"))
                .outputMapping(Map.of())
                .build();

            // 4. 审批通过节点
            Node approve = Node.builder()
                .id("approve")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'审批通过'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "output.result"))
                .build();

            // 5. 拒绝节点
            Node reject = Node.builder()
                .id("reject")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'审批拒绝'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "output.result"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("risk-assessment-wf")
                .name("Risk Assessment")
                .startNodeId("fetch_credit")
                .nodes(List.of(fetchCredit, riskScore, decision, approve, reject))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("fetch_credit").toNodeId("risk_score").build(),
                    Edge.builder().id("e2").fromNodeId("risk_score").toNodeId("decision").build(),
                    Edge.builder().id("e3").fromNodeId("decision").toNodeId("approve").build(),
                    Edge.builder().id("e4").fromNodeId("decision").toNodeId("reject").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("risk-assessment-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getFinalContext().get("output.result").map(Object::toString))
                .contains("审批通过");
        }
    }

    @Nested
    @DisplayName("Scenario 2: Loan Approval with Try-Catch (贷款审批)")
    class LoanApprovalScenario {

        @Test
        @DisplayName("Should handle loan approval with try-catch")
        void should_handle_loan_approval_with_try_catch() {
            // 1. 接收申请
            Node submit = Node.builder()
                .id("submit")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "status", "expression", "'submitted'", "nextNodeId", "try_catch"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("status", "status"))
                .build();

            // 2. TRY_CATCH
            Node tryCatch = Node.builder()
                .id("try_catch")
                .type(NodeType.TRY_CATCH)
                .config(Map.of(
                    "tryNodeId", "process",
                    "catchNodeId", "handle_error",
                    "nextNodeId", "notify"
                ))
                .inputMapping(Map.of())
                .outputMapping(Map.of("caught", "error.caught"))
                .build();

            // 3. 正常处理
            Node process = Node.builder()
                .id("process")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'贷款处理中'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "result"))
                .build();

            // 4. 异常处理
            Node handleError = Node.builder()
                .id("handle_error")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "errorMsg", "expression", "'已捕获异常'", "nextNodeId", "notify"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("errorMsg", "errorMsg"))
                .build();

            // 5. 通知
            Node notify = Node.builder()
                .id("notify")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "done", "expression", "'完成'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("done", "output.done"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("loan-approval-wf")
                .name("Loan Approval")
                .startNodeId("submit")
                .nodes(List.of(submit, tryCatch, process, handleError, notify))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("submit").toNodeId("try_catch").build(),
                    Edge.builder().id("e2").fromNodeId("try_catch").toNodeId("process").build(),
                    Edge.builder().id("e3").fromNodeId("process").toNodeId("handle_error").build(),
                    Edge.builder().id("e4").fromNodeId("handle_error").toNodeId("notify").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("loan-approval-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Scenario 3: Anti-Fraud Detection (反欺诈检测)")
    class AntiFraudScenario {

        @Test
        @DisplayName("Should detect fraud in parallel")
        void should_detect_fraud_in_parallel() {
            // 1. 获取交易
            Node getTransaction = Node.builder()
                .id("get_transaction")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "transaction", "expression", "'TX123456'", "nextNodeId", "parallel_check"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("transaction", "transaction"))
                .build();

            // 2. PARALLEL并行检测
            Map<String, Object> parallelConfig = new HashMap<>();
            parallelConfig.put("branches", List.of(
                Map.of("name", "amount", "nodeIds", List.of("check_amount")),
                Map.of("name", "frequency", "nodeIds", List.of("check_frequency")),
                Map.of("name", "location", "nodeIds", List.of("check_location"))
            ));
            parallelConfig.put("strategy", "AND");
            parallelConfig.put("nextNodeId", "decision");

            Node parallelCheck = Node.builder()
                .id("parallel_check")
                .type(NodeType.PARALLEL)
                .config(parallelConfig)
                .inputMapping(Map.of())
                .outputMapping(Map.of())
                .build();

            // 3. 各检测节点
            Node checkAmount = Node.builder()
                .id("check_amount")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "amountOK", "expression", "true"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("amountOK", "amountOK"))
                .build();

            Node checkFrequency = Node.builder()
                .id("check_frequency")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "freqOK", "expression", "true"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("freqOK", "freqOK"))
                .build();

            Node checkLocation = Node.builder()
                .id("check_location")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "locationOK", "expression", "true"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("locationOK", "locationOK"))
                .build();

            // 4. 决策
            List<Map<String, String>> decisionConditions = new ArrayList<>();
            decisionConditions.add(Map.of("expression", "#input.amountOK == true", "nextNodeId", "pass"));
            decisionConditions.add(Map.of("expression", "#input.amountOK == false", "nextNodeId", "block"));

            Node decision = Node.builder()
                .id("decision")
                .type(NodeType.CONDITION)
                .config(Map.of(
                    "conditions", decisionConditions,
                    "defaultNextNodeId", "pass"
                ))
                .inputMapping(Map.of())
                .outputMapping(Map.of())
                .build();

            // 5. 通过
            Node pass = Node.builder()
                .id("pass")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'交易通过'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "output.result"))
                .build();

            // 6. 拦截
            Node block = Node.builder()
                .id("block")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'交易拦截'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "output.result"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("anti-fraud-wf")
                .name("Anti-Fraud")
                .startNodeId("get_transaction")
                .nodes(List.of(getTransaction, parallelCheck, checkAmount, checkFrequency, checkLocation, decision, pass, block))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("get_transaction").toNodeId("parallel_check").build(),
                    Edge.builder().id("e2").fromNodeId("parallel_check").toNodeId("check_amount").build(),
                    Edge.builder().id("e3").fromNodeId("parallel_check").toNodeId("check_frequency").build(),
                    Edge.builder().id("e4").fromNodeId("parallel_check").toNodeId("check_location").build(),
                    Edge.builder().id("e5").fromNodeId("check_amount").toNodeId("decision").build(),
                    Edge.builder().id("e6").fromNodeId("check_frequency").toNodeId("decision").build(),
                    Edge.builder().id("e7").fromNodeId("check_location").toNodeId("decision").build(),
                    Edge.builder().id("e8").fromNodeId("decision").toNodeId("pass").build(),
                    Edge.builder().id("e9").fromNodeId("decision").toNodeId("block").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("anti-fraud-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Scenario 4: Account Audit (账户审核)")
    class AccountAuditScenario {

        @Test
        @DisplayName("Should audit account with subworkflow")
        void should_audit_account_with_subworkflow() {
            // 1. 提取信息
            Node extractInfo = Node.builder()
                .id("extract_info")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "extracted", "expression", "'信息已提取'", "nextNodeId", "deep_audit"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("extracted", "extracted"))
                .build();

            // 2. SUBWORKFLOW调用子工作流
            Node subworkflow = Node.builder()
                .id("deep_audit")
                .type(NodeType.SUBWORKFLOW)
                .config(Map.of(
                    "workflowId", "account-audit-subwf",
                    "nextNodeId", "notify"
                ))
                .inputMapping(Map.of())
                .outputMapping(Map.of("auditResult", "auditResult"))
                .build();

            // 3. 通知
            Node notify = Node.builder()
                .id("notify")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "done", "expression", "'审核完成'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("done", "output.done"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("account-audit-wf")
                .name("Account Audit")
                .startNodeId("extract_info")
                .nodes(List.of(extractInfo, subworkflow, notify))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("extract_info").toNodeId("deep_audit").build(),
                    Edge.builder().id("e2").fromNodeId("deep_audit").toNodeId("notify").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("account-audit-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
        }
    }
}
