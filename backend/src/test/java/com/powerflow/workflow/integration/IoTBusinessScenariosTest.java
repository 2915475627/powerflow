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
 * IoT Business Scenarios Integration Tests
 *
 * Tests based on the IoT MVP business scenarios design document.
 * These tests verify the workflow engine can handle:
 * - Device control with FOREACH iteration
 * - Parallel data collection with threshold alerts
 * - Multi-level alert triggering with SUBWORKFLOW
 * - Remote diagnosis with LLM and CONDITION routing
 */
class IoTBusinessScenariosTest {

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
    @DisplayName("Scenario 1: Device Control (设备管控)")
    class DeviceControlScenario {

        @Test
        @DisplayName("Should control devices in foreach loop")
        void should_control_devices_in_foreach() {
            // 1. FOREACH遍历设备列表
            Node foreachDevice = Node.builder()
                .id("foreach_device")
                .type(NodeType.FOREACH)
                .config(Map.of(
                    "collection", "#input.devices",
                    "variableName", "device",
                    "maxIterations", 100,
                    "nextNodeId", "aggregate"
                ))
                .inputMapping(Map.of("devices", "devices"))
                .outputMapping(Map.of("results", "commandResults"))
                .build();

            // 2. 汇总结果
            Node aggregate = Node.builder()
                .id("aggregate")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "count", "expression", "#input.totalIterations"))
                .inputMapping(Map.of("totalIterations", "commandResults.totalIterations"))
                .outputMapping(Map.of("count", "output.count"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("device-control-wf")
                .name("Device Control")
                .startNodeId("foreach_device")
                .nodes(List.of(foreachDevice, aggregate))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("foreach_device").toNodeId("aggregate").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of("devices", List.of("设备A", "设备B", "设备C")));
            WorkflowExecutionResult result = workflowExecutor.execute("device-control-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Scenario 2: Data Collection (数据采集)")
    class DataCollectionScenario {

        @Test
        @DisplayName("Should collect data with threshold alert")
        void should_collect_data_with_threshold() {
            // 1. PARALLEL并行采集
            Node parallelCollect = Node.builder()
                .id("parallel_collect")
                .type(NodeType.PARALLEL)
                .config(Map.of(
                    "branches", List.of(
                        Map.of("name", "temp", "nodeIds", List.of("temp_node")),
                        Map.of("name", "humidity", "nodeIds", List.of("humidity_node")),
                        Map.of("name", "pressure", "nodeIds", List.of("pressure_node"))
                    ),
                    "strategy", "AND",
                    "nextNodeId", "check_threshold"
                ))
                .inputMapping(Map.of())
                .outputMapping(Map.of())
                .build();

            // 2. 采集节点
            Node tempNode = Node.builder()
                .id("temp_node")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "temperature", "expression", "25"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("temperature", "temperature"))
                .build();

            Node humidityNode = Node.builder()
                .id("humidity_node")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "humidity", "expression", "60"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("humidity", "humidity"))
                .build();

            Node pressureNode = Node.builder()
                .id("pressure_node")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "pressure", "expression", "1013"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("pressure", "pressure"))
                .build();

            // 3. CONDITION阈值判断
            Node checkThreshold = Node.builder()
                .id("check_threshold")
                .type(NodeType.CONDITION)
                .config(Map.of(
                    "conditions", List.of(
                        Map.of("expression", "#input.temperature > 30", "nextNodeId", "alert"),
                        Map.of("expression", "#input.temperature <= 30", "nextNodeId", "normal")
                    ),
                    "defaultNextNodeId", "normal"
                ))
                .inputMapping(Map.of("temperature", "temperature"))
                .outputMapping(Map.of())
                .build();

            // 4. 告警
            Node alert = Node.builder()
                .id("alert")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'温度告警'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "output.result"))
                .build();

            // 5. 正常
            Node normal = Node.builder()
                .id("normal")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'数据采集正常'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "output.result"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("data-collection-wf")
                .name("Data Collection")
                .startNodeId("parallel_collect")
                .nodes(List.of(parallelCollect, tempNode, humidityNode, pressureNode, checkThreshold, alert, normal))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("parallel_collect").toNodeId("temp_node").build(),
                    Edge.builder().id("e2").fromNodeId("parallel_collect").toNodeId("humidity_node").build(),
                    Edge.builder().id("e3").fromNodeId("parallel_collect").toNodeId("pressure_node").build(),
                    Edge.builder().id("e4").fromNodeId("temp_node").toNodeId("check_threshold").build(),
                    Edge.builder().id("e5").fromNodeId("humidity_node").toNodeId("check_threshold").build(),
                    Edge.builder().id("e6").fromNodeId("pressure_node").toNodeId("check_threshold").build(),
                    Edge.builder().id("e7").fromNodeId("check_threshold").toNodeId("alert").build(),
                    Edge.builder().id("e8").fromNodeId("check_threshold").toNodeId("normal").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("data-collection-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Scenario 3: Alert Trigger (告警触发)")
    class AlertTriggerScenario {

        @Test
        @DisplayName("Should trigger multi-level alerts")
        void should_trigger_multi_level_alerts() {
            // 1. 接收告警
            Node receiveAlert = Node.builder()
                .id("receive_alert")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "level", "expression", "2", "nextNodeId", "severity"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("level", "level"))
                .build();

            // 2. CONDITION判断级别
            Node severity = Node.builder()
                .id("severity")
                .type(NodeType.CONDITION)
                .config(Map.of(
                    "conditions", List.of(
                        Map.of("expression", "#input.level == 1", "nextNodeId", "notify_l1"),
                        Map.of("expression", "#input.level == 2", "nextNodeId", "notify_l2"),
                        Map.of("expression", "#input.level == 3", "nextNodeId", "notify_l3")
                    ),
                    "defaultNextNodeId", "notify_l1"
                ))
                .inputMapping(Map.of("level", "level"))
                .outputMapping(Map.of())
                .build();

            // 3. L1通知
            Node notifyL1 = Node.builder()
                .id("notify_l1")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'L1通知已发送'", "nextNodeId", "handle"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "result"))
                .build();

            // 4. L2通知
            Node notifyL2 = Node.builder()
                .id("notify_l2")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'L2通知已发送'", "nextNodeId", "handle"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "result"))
                .build();

            // 5. L3紧急
            Node notifyL3 = Node.builder()
                .id("notify_l3")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'L3紧急通知已发送'", "nextNodeId", "handle"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "result"))
                .build();

            // 6. SUBWORKFLOW自动处理
            Node handle = Node.builder()
                .id("handle")
                .type(NodeType.SUBWORKFLOW)
                .config(Map.of("workflowId", "auto-handle-wf", "nextNodeId", "complete"))
                .inputMapping(Map.of())
                .outputMapping(Map.of())
                .build();

            // 7. 完成
            Node complete = Node.builder()
                .id("complete")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "done", "expression", "'告警处理完成'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("done", "output.done"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("alert-trigger-wf")
                .name("Alert Trigger")
                .startNodeId("receive_alert")
                .nodes(List.of(receiveAlert, severity, notifyL1, notifyL2, notifyL3, handle, complete))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("receive_alert").toNodeId("severity").build(),
                    Edge.builder().id("e2").fromNodeId("severity").toNodeId("notify_l1").build(),
                    Edge.builder().id("e3").fromNodeId("severity").toNodeId("notify_l2").build(),
                    Edge.builder().id("e4").fromNodeId("severity").toNodeId("notify_l3").build(),
                    Edge.builder().id("e5").fromNodeId("notify_l1").toNodeId("handle").build(),
                    Edge.builder().id("e6").fromNodeId("notify_l2").toNodeId("handle").build(),
                    Edge.builder().id("e7").fromNodeId("notify_l3").toNodeId("handle").build(),
                    Edge.builder().id("e8").fromNodeId("handle").toNodeId("complete").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("alert-trigger-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Scenario 4: Remote Diagnosis (远程诊断)")
    class RemoteDiagnosisScenario {

        @Test
        @DisplayName("Should diagnose with llm and condition")
        void should_diagnose_with_condition() {
            // 1. LLM解析症状
            Node parseSymptom = Node.builder()
                .id("parse_symptom")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "symptom", "expression", "'设备无法启动'", "nextNodeId", "fetch_history"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("symptom", "symptom"))
                .build();

            // 2. HTTP获取诊断历史
            Node fetchHistory = Node.builder()
                .id("fetch_history")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "history", "expression", "'历史诊断记录'", "nextNodeId", "diagnose"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("history", "history"))
                .build();

            // 3. LLM诊断建议
            Node diagnose = Node.builder()
                .id("diagnose")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "solution", "expression", "'有解决方案'", "nextNodeId", "has_solution"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("solution", "solution"))
                .build();

            // 4. CONDITION是否有方案
            Node hasSolution = Node.builder()
                .id("has_solution")
                .type(NodeType.CONDITION)
                .config(Map.of(
                    "conditions", List.of(
                        Map.of("expression", "#input.solution != null", "nextNodeId", "execute"),
                        Map.of("expression", "#input.solution == null", "nextNodeId", "escalate")
                    ),
                    "defaultNextNodeId", "escalate"
                ))
                .inputMapping(Map.of("solution", "solution"))
                .outputMapping(Map.of())
                .build();

            // 5. 执行方案
            Node execute = Node.builder()
                .id("execute")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'诊断完成'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "output.result"))
                .build();

            // 6. 升级
            Node escalate = Node.builder()
                .id("escalate")
                .type(NodeType.DATA_PROCESSING)
                .config(Map.of("outputKey", "result", "expression", "'已升级人工处理'"))
                .inputMapping(Map.of())
                .outputMapping(Map.of("result", "output.result"))
                .build();

            Workflow workflow = Workflow.builder()
                .id("remote-diagnosis-wf")
                .name("Remote Diagnosis")
                .startNodeId("parse_symptom")
                .nodes(List.of(parseSymptom, fetchHistory, diagnose, hasSolution, execute, escalate))
                .edges(List.of(
                    Edge.builder().id("e1").fromNodeId("parse_symptom").toNodeId("fetch_history").build(),
                    Edge.builder().id("e2").fromNodeId("fetch_history").toNodeId("diagnose").build(),
                    Edge.builder().id("e3").fromNodeId("diagnose").toNodeId("has_solution").build(),
                    Edge.builder().id("e4").fromNodeId("has_solution").toNodeId("execute").build(),
                    Edge.builder().id("e5").fromNodeId("has_solution").toNodeId("escalate").build()
                ))
                .build();

            workflowRepository.save(workflow);
            Context inputContext = new Context(Map.of());
            WorkflowExecutionResult result = workflowExecutor.execute("remote-diagnosis-wf", inputContext);

            assertThat(result.isSuccess()).isTrue();
        }
    }
}
