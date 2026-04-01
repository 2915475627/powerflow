package com.powerflow.workflow.adapter.outbound.persistence;

import com.powerflow.workflow.domain.model.Edge;
import com.powerflow.workflow.domain.model.Node;
import com.powerflow.workflow.domain.model.Workflow;
import com.powerflow.workflow.domain.model.enums.NodeType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 初始化示例工作流数据到内存仓库
 * 应用启动时自动加载，预示给前端展示
 */
@Component
public class WorkflowDataInitializer implements CommandLineRunner {

    private final InMemoryWorkflowRepository workflowRepository;

    public WorkflowDataInitializer(InMemoryWorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    @Override
    public void run(String... args) {
        initializeWorkflows();
    }

    private void initializeWorkflows() {
        // ==================== 电商场景 ====================
        createWorkflow(
            "ecommerce-customer-service",
            "智能客服路由",
            "LLM意图识别+多分支路由",
            "start-1",
            List.of(
                createStartNode("start-1", "开始", "NONE", null, null, null),
                // LLM_CALL uses "prompt" not "expression", needs nextNodeId
                createLlmNode("intent_node", "意图识别",
                    Map.of("outputKey", "intent", "prompt", "'search'", "nextNodeId", "route")),
                // BRANCH needs defaultNextNodeId
                createBranchNode("route", "路由分支",
                    List.of(
                        Map.of("name", "搜索", "expression", "#input.intent == 'search'", "nextNodeId", "search_node"),
                        Map.of("name", "推荐", "expression", "#input.intent == 'recommend'", "nextNodeId", "recommend_node"),
                        Map.of("name", "咨询", "expression", "#input.intent == 'inquiry'", "nextNodeId", "inquiry_node"),
                        Map.of("name", "订单", "expression", "#input.intent == 'order'", "nextNodeId", "order_node")
                    ),
                    "end-1"),
                // Terminal nodes can omit nextNodeId
                createDataNode("search_node", "搜索商品", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "result", "expression", "'搜索结果: Nike运动鞋'", "nextNodeId", "end-1")),
                createDataNode("recommend_node", "商品推荐", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "result", "expression", "'推荐结果: 热销商品'", "nextNodeId", "end-1")),
                createDataNode("inquiry_node", "库存咨询", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "result", "expression", "'库存充足'", "nextNodeId", "end-1")),
                createDataNode("order_node", "下单处理", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "result", "expression", "'订单已创建'", "nextNodeId", "end-1")),
                createEndNode("end-1", "结束")
            ),
            List.of(
                createEdge("s1", "start-1", "intent_node"),
                createEdge("e1", "intent_node", "route"),
                createEdge("e2", "route", "search_node"),
                createEdge("e3", "route", "recommend_node"),
                createEdge("e4", "route", "inquiry_node"),
                createEdge("e5", "route", "order_node"),
                createEdge("e6", "search_node", "end-1"),
                createEdge("e7", "recommend_node", "end-1"),
                createEdge("e8", "inquiry_node", "end-1"),
                createEdge("e9", "order_node", "end-1")
            )
        );

        createWorkflow(
            "ecommerce-order-create",
            "订单创建流程",
            "库存检查+条件分支+订单创建",
            "start",
            List.of(
                createStartNode("start", "开始", "NONE", null, null, null),
                createDataNode("check_stock", "检查库存", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "sufficient", "expression", "true", "nextNodeId", "condition")),
                createConditionNode("condition", "库存判断",
                    List.of(
                        Map.of("expression", "#input.sufficient == true", "nextNodeId", "create_order"),
                        Map.of("expression", "#input.sufficient == false", "nextNodeId", "insufficient")
                    ),
                    "end_node"),
                createDataNode("create_order", "创建订单", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "orderId", "expression", "'ORD202604010001'", "nextNodeId", "end_node")),
                createDataNode("insufficient", "库存不足", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "error", "expression", "'库存不足'", "nextNodeId", "end_node")),
                createEndNode("end_node", "结束")
            ),
            List.of(
                createEdge("e1", "start", "check_stock"),
                createEdge("e2", "check_stock", "condition"),
                createEdge("e3", "condition", "create_order"),
                createEdge("e4", "condition", "insufficient"),
                createEdge("e5", "create_order", "end_node"),
                createEdge("e6", "insufficient", "end_node")
            )
        );

        // ==================== 金融场景 ====================
        createWorkflow(
            "finance-risk-assessment",
            "风控评估",
            "多维度评分+自动决策",
            "start",
            List.of(
                createStartNode("start", "开始", "NONE", null, null, null),
                createDataNode("fetch_credit", "获取征信", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "creditScore", "expression", "750", "nextNodeId", "risk_score")),
                createDataNode("risk_score", "风控评分", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "riskLevel", "expression", "0.85", "nextNodeId", "decision")),
                createConditionNode("decision", "决策判断",
                    List.of(
                        Map.of("expression", "#input.riskLevel >= 0.7", "nextNodeId", "approve"),
                        Map.of("expression", "#input.riskLevel < 0.7", "nextNodeId", "reject")
                    ),
                    "end_node"),
                createDataNode("approve", "审批通过", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "result", "expression", "'审批通过'", "nextNodeId", "end_node")),
                createDataNode("reject", "审批拒绝", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "result", "expression", "'审批拒绝'", "nextNodeId", "end_node")),
                createEndNode("end_node", "结束")
            ),
            List.of(
                createEdge("e1", "start", "fetch_credit"),
                createEdge("e2", "fetch_credit", "risk_score"),
                createEdge("e3", "risk_score", "decision"),
                createEdge("e4", "decision", "approve"),
                createEdge("e5", "decision", "reject"),
                createEdge("e6", "approve", "end_node"),
                createEdge("e7", "reject", "end_node")
            )
        );

        createWorkflow(
            "finance-anti-fraud",
            "反欺诈检测",
            "并行多维度检测",
            "start",
            List.of(
                createStartNode("start", "开始", "NONE", null, null, null),
                createDataNode("get_transaction", "获取交易", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "transaction", "expression", "'TX123456'", "nextNodeId", "parallel")),
                createParallelNode("parallel", "并行检测",
                    List.of(
                        Map.of("name", "金额检测", "nodeIds", List.of("check_amount")),
                        Map.of("name", "频率检测", "nodeIds", List.of("check_frequency")),
                        Map.of("name", "位置检测", "nodeIds", List.of("check_location"))
                    ),
                    "join"),
                createDataNode("check_amount", "金额异常检测", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "amountOK", "expression", "true", "nextNodeId", "join")),
                createDataNode("check_frequency", "频率异常检测", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "freqOK", "expression", "true", "nextNodeId", "join")),
                createDataNode("check_location", "位置异常检测", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "locationOK", "expression", "true", "nextNodeId", "join")),
                createJoinNode("join", "汇聚"),
                createConditionNode("decision", "决策",
                    List.of(
                        Map.of("expression", "#input.amountOK == true", "nextNodeId", "pass")
                    ),
                    "end_node"),
                createDataNode("pass", "交易通过", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "result", "expression", "'交易通过'", "nextNodeId", "end_node")),
                createDataNode("block", "交易拦截", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "result", "expression", "'交易拦截'", "nextNodeId", "end_node")),
                createEndNode("end_node", "结束")
            ),
            List.of(
                createEdge("e1", "start", "get_transaction"),
                createEdge("e2", "get_transaction", "parallel"),
                createEdge("e3", "parallel", "check_amount"),
                createEdge("e4", "parallel", "check_frequency"),
                createEdge("e5", "parallel", "check_location"),
                createEdge("e6", "check_amount", "join"),
                createEdge("e7", "check_frequency", "join"),
                createEdge("e8", "check_location", "join"),
                createEdge("e9", "join", "decision"),
                createEdge("e10", "decision", "pass"),
                createEdge("e11", "decision", "block"),
                createEdge("e12", "pass", "end_node"),
                createEdge("e13", "block", "end_node")
            )
        );

        // ==================== 企业办公场景 ====================
        createWorkflow(
            "enterprise-approval",
            "多级审批流",
            "SUBWORKFLOW嵌套多级审批",
            "start",
            List.of(
                createStartNode("start", "开始", "NONE", null, null, null),
                createDataNode("receive", "接收申请", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "request", "expression", "'申请数据'", "nextNodeId", "level1")),
                createDataNode("level1", "一级审批", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "level1Result", "expression", "'一级审批通过'", "nextNodeId", "level2")),
                createSubworkflowNode("level2", "二级审批", "approval-level2-wf", "notify"),
                createDataNode("notify", "通知", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "done", "expression", "'审批流程完成'", "nextNodeId", "end_node")),
                createEndNode("end_node", "结束")
            ),
            List.of(
                createEdge("e1", "start", "receive"),
                createEdge("e2", "receive", "level1"),
                createEdge("e3", "level1", "level2"),
                createEdge("e4", "level2", "notify"),
                createEdge("e5", "notify", "end_node")
            )
        );

        createWorkflow(
            "enterprise-report",
            "数据报表生成",
            "PARALLEL采集+JOIN汇聚",
            "start",
            List.of(
                createStartNode("start", "开始", "NONE", null, null, null),
                createParallelNode("parallel_fetch", "并行采集",
                    List.of(
                        Map.of("name", "销售数据", "nodeIds", List.of("sales_node")),
                        Map.of("name", "库存数据", "nodeIds", List.of("inventory_node")),
                        Map.of("name", "用户数据", "nodeIds", List.of("users_node"))
                    ),
                    "join"),
                createDataNode("sales_node", "采集销售", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "sales", "expression", "10000", "nextNodeId", "join")),
                createDataNode("inventory_node", "采集库存", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "inventory", "expression", "5000", "nextNodeId", "join")),
                createDataNode("users_node", "采集用户", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "users", "expression", "1000", "nextNodeId", "join")),
                createJoinNode("join", "汇聚"),
                createDataNode("aggregate", "汇总", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "report", "expression", "'报表生成完成'", "nextNodeId", "end_node")),
                createEndNode("end_node", "结束")
            ),
            List.of(
                createEdge("e1", "start", "parallel_fetch"),
                createEdge("e2", "parallel_fetch", "sales_node"),
                createEdge("e3", "parallel_fetch", "inventory_node"),
                createEdge("e4", "parallel_fetch", "users_node"),
                createEdge("e5", "sales_node", "join"),
                createEdge("e6", "inventory_node", "join"),
                createEdge("e7", "users_node", "join"),
                createEdge("e8", "join", "aggregate"),
                createEdge("e9", "aggregate", "end_node")
            )
        );

        // ==================== 社交内容场景 ====================
        createWorkflow(
            "social-content-moderation",
            "内容审核",
            "三级分类审核流程",
            "start",
            List.of(
                createStartNode("start", "开始", "NONE", null, null, null),
                createDataNode("get_content", "获取内容", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "content", "expression", "'用户发布内容'", "nextNodeId", "audit")),
                createDataNode("audit", "LLM审核", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "auditResult", "expression", "'pass'", "nextNodeId", "decision")),
                createConditionNode("decision", "审核决策",
                    List.of(
                        Map.of("expression", "#input.auditResult == 'pass'", "nextNodeId", "publish"),
                        Map.of("expression", "#input.auditResult == 'review'", "nextNodeId", "manual_review"),
                        Map.of("expression", "#input.auditResult == 'block'", "nextNodeId", "block")
                    ),
                    "end_node"),
                createDataNode("publish", "发布", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "result", "expression", "'内容已发布'", "nextNodeId", "end_node")),
                createDataNode("manual_review", "人工复审", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "result", "expression", "'需人工复审'", "nextNodeId", "end_node")),
                createDataNode("block", "拦截", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "result", "expression", "'内容已拦截'", "nextNodeId", "end_node")),
                createEndNode("end_node", "结束")
            ),
            List.of(
                createEdge("e1", "start", "get_content"),
                createEdge("e2", "get_content", "audit"),
                createEdge("e3", "audit", "decision"),
                createEdge("e4", "decision", "publish"),
                createEdge("e5", "decision", "manual_review"),
                createEdge("e6", "decision", "block"),
                createEdge("e7", "publish", "end_node"),
                createEdge("e8", "manual_review", "end_node"),
                createEdge("e9", "block", "end_node")
            )
        );

        createWorkflow(
            "social-user-profile",
            "用户画像构建",
            "PARALLEL四路采集+JOIN汇聚",
            "start",
            List.of(
                createStartNode("start", "开始", "NONE", null, null, null),
                createParallelNode("parallel_fetch", "并行采集",
                    List.of(
                        Map.of("name", "基本信息", "nodeIds", List.of("basic_node")),
                        Map.of("name", "行为数据", "nodeIds", List.of("behavior_node")),
                        Map.of("name", "兴趣数据", "nodeIds", List.of("interest_node")),
                        Map.of("name", "社交数据", "nodeIds", List.of("social_node"))
                    ),
                    "join"),
                createDataNode("basic_node", "基本信息", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "basic", "expression", "'姓名: 张三'", "nextNodeId", "join")),
                createDataNode("behavior_node", "行为数据", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "behavior", "expression", "'月消费: 5000'", "nextNodeId", "join")),
                createDataNode("interest_node", "兴趣数据", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "interest", "expression", "'运动、科技'", "nextNodeId", "join")),
                createDataNode("social_node", "社交数据", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "social", "expression", "'朋友圈活跃'", "nextNodeId", "join")),
                createJoinNode("join", "汇聚"),
                createDataNode("merge", "合并画像", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "profile", "expression", "'画像已生成'", "nextNodeId", "end_node")),
                createEndNode("end_node", "结束")
            ),
            List.of(
                createEdge("e1", "start", "parallel_fetch"),
                createEdge("e2", "parallel_fetch", "basic_node"),
                createEdge("e3", "parallel_fetch", "behavior_node"),
                createEdge("e4", "parallel_fetch", "interest_node"),
                createEdge("e5", "parallel_fetch", "social_node"),
                createEdge("e6", "basic_node", "join"),
                createEdge("e7", "behavior_node", "join"),
                createEdge("e8", "interest_node", "join"),
                createEdge("e9", "social_node", "join"),
                createEdge("e10", "join", "merge"),
                createEdge("e11", "merge", "end_node")
            )
        );

        // ==================== 物联网场景 ====================
        createWorkflow(
            "iot-device-control",
            "设备管控",
            "FOREACH批量控制",
            "start",
            List.of(
                createStartNode("start", "开始", "NONE", null, null, null),
                createDataNode("get_devices", "获取设备", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "devices", "expression", "T(java.util.Arrays).asList('设备A','设备B','设备C')", "nextNodeId", "foreach_device")),
                createForeachNode("foreach_device", "遍历控制", "#input.devices", "device", 100, "check_result"),
                createConditionNode("check_result", "结果检查",
                    List.of(
                        Map.of("expression", "#input.success == true", "nextNodeId", "next"),
                        Map.of("expression", "#input.success == false", "nextNodeId", "end_node")
                    ),
                    "next"),
                createDataNode("next", "下一步", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "done", "expression", "'设备控制完成'", "nextNodeId", "end_node")),
                createEndNode("end_node", "结束")
            ),
            List.of(
                createEdge("e1", "start", "get_devices"),
                createEdge("e2", "get_devices", "foreach_device"),
                createEdge("e3", "foreach_device", "check_result"),
                createEdge("e4", "check_result", "next"),
                createEdge("e5", "check_result", "end_node"),
                createEdge("e6", "next", "end_node")
            )
        );

        createWorkflow(
            "iot-alert-trigger",
            "告警触发",
            "多级告警+SUBWORKFLOW处理",
            "start",
            List.of(
                createStartNode("start", "开始", "NONE", null, null, null),
                createDataNode("receive_alert", "接收告警", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "level", "expression", "2", "nextNodeId", "severity")),
                createConditionNode("severity", "告警级别",
                    List.of(
                        Map.of("expression", "#input.level == 1", "nextNodeId", "notify_l1"),
                        Map.of("expression", "#input.level == 2", "nextNodeId", "notify_l2"),
                        Map.of("expression", "#input.level == 3", "nextNodeId", "notify_l3")
                    ),
                    "end_node"),
                createDataNode("notify_l1", "L1通知", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "result", "expression", "'L1通知已发送'", "nextNodeId", "handle")),
                createDataNode("notify_l2", "L2通知", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "result", "expression", "'L2通知已发送'", "nextNodeId", "handle")),
                createDataNode("notify_l3", "L3紧急", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "result", "expression", "'L3紧急通知已发送'", "nextNodeId", "handle")),
                createSubworkflowNode("handle", "自动处理", "auto-handle-wf", "end_node"),
                createEndNode("end_node", "结束")
            ),
            List.of(
                createEdge("e1", "start", "receive_alert"),
                createEdge("e2", "receive_alert", "severity"),
                createEdge("e3", "severity", "notify_l1"),
                createEdge("e4", "severity", "notify_l2"),
                createEdge("e5", "severity", "notify_l3"),
                createEdge("e6", "notify_l1", "handle"),
                createEdge("e7", "notify_l2", "handle"),
                createEdge("e8", "notify_l3", "handle"),
                createEdge("e9", "handle", "end_node")
            )
        );

        // ==================== 定时触发场景 ====================
        createWorkflow(
            "schedule-notification",
            "每日定时发送通知",
            "START节点+CRON定时触发",
            "start",
            List.of(
                createStartNode("start", "开始", "CRON", "0 9 * * *", null, null),
                createDataNode("prepare", "准备通知", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "content", "expression", "'每日报表通知'", "nextNodeId", "send")),
                createDataNode("send", "发送通知", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "result", "expression", "'通知已发送'", "nextNodeId", "end_node")),
                createEndNode("end_node", "结束")
            ),
            List.of(
                createEdge("e1", "start", "prepare"),
                createEdge("e2", "prepare", "send"),
                createEdge("e3", "send", "end_node")
            )
        );

        // ==================== Webhook触发场景 ====================
        createWorkflow(
            "webhook-order",
            "接收订单Webhook",
            "START节点+WEBHOOK触发",
            "start",
            List.of(
                createStartNode("start", "开始", "WEBHOOK", null, "/webhook/order",
                    Map.of("orderId", "order_id", "amount", "order_amount")),
                createDataNode("validate", "验证订单", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "valid", "expression", "true", "nextNodeId", "process")),
                createDataNode("process", "处理订单", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "result", "expression", "'订单处理完成'", "nextNodeId", "end_node")),
                createEndNode("end_node", "结束")
            ),
            List.of(
                createEdge("e1", "start", "validate"),
                createEdge("e2", "validate", "process"),
                createEdge("e3", "process", "end_node")
            )
        );

        // ==================== 公共服务场景 ====================
        createWorkflow(
            "public-form-processing",
            "表单处理",
            "条件分流处理",
            "start",
            List.of(
                createStartNode("start", "开始", "NONE", null, null, null),
                createDataNode("receive_form", "接收表单", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "formData", "expression", "'表单数据'", "nextNodeId", "validate")),
                createDataNode("validate", "表单验证", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "formType", "expression", "'A'", "nextNodeId", "route")),
                createConditionNode("route", "业务分流",
                    List.of(
                        Map.of("expression", "#input.formType == 'A'", "nextNodeId", "handle_a"),
                        Map.of("expression", "#input.formType == 'B'", "nextNodeId", "handle_b")
                    ),
                    "handle_other"),
                createDataNode("handle_a", "业务A处理", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "result", "expression", "'业务A处理完成'", "nextNodeId", "end_node")),
                createDataNode("handle_b", "业务B处理", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "result", "expression", "'业务B处理完成'", "nextNodeId", "end_node")),
                createDataNode("handle_other", "其他业务", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "result", "expression", "'其他业务处理'", "nextNodeId", "end_node")),
                createEndNode("end_node", "结束")
            ),
            List.of(
                createEdge("e1", "start", "receive_form"),
                createEdge("e2", "receive_form", "validate"),
                createEdge("e3", "validate", "route"),
                createEdge("e4", "route", "handle_a"),
                createEdge("e5", "route", "handle_b"),
                createEdge("e6", "route", "handle_other"),
                createEdge("e7", "handle_a", "end_node"),
                createEdge("e8", "handle_b", "end_node"),
                createEdge("e9", "handle_other", "end_node")
            )
        );

        createWorkflow(
            "public-complaint",
            "投诉处理",
            "LLM分类+紧急分流+SUBWORKFLOW",
            "start",
            List.of(
                createStartNode("start", "开始", "NONE", null, null, null),
                createDataNode("receive_complaint", "接收投诉", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "complaint", "expression", "'用户投诉内容'", "nextNodeId", "classify")),
                createDataNode("classify", "LLM分类", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "urgent", "expression", "true", "nextNodeId", "urgency")),
                createConditionNode("urgency", "紧急程度",
                    List.of(
                        Map.of("expression", "#input.urgent == true", "nextNodeId", "dispatch_urgent"),
                        Map.of("expression", "#input.urgent == false", "nextNodeId", "dispatch_normal")
                    ),
                    "end_node"),
                createDataNode("dispatch_urgent", "紧急派发", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "result", "expression", "'紧急派发'", "nextNodeId", "track")),
                createDataNode("dispatch_normal", "普通派发", NodeType.DATA_PROCESSING,
                    Map.of("outputKey", "result", "expression", "'普通派发'", "nextNodeId", "track")),
                createSubworkflowNode("track", "进度跟踪", "complaint-track-wf", "end_node"),
                createEndNode("end_node", "结束")
            ),
            List.of(
                createEdge("e1", "start", "receive_complaint"),
                createEdge("e2", "receive_complaint", "classify"),
                createEdge("e3", "classify", "urgency"),
                createEdge("e4", "urgency", "dispatch_urgent"),
                createEdge("e5", "urgency", "dispatch_normal"),
                createEdge("e6", "dispatch_urgent", "track"),
                createEdge("e7", "dispatch_normal", "track"),
                createEdge("e8", "track", "end_node")
            )
        );

        System.out.println("=== 初始化完成: " + workflowRepository.findAll().size() + " 个工作流已加载 ===");
    }

    // ==================== 辅助方法 ====================

    private void createWorkflow(String id, String name, String description, String startNodeId,
                                List<Node> nodes, List<Edge> edges) {
        Workflow workflow = Workflow.builder()
            .id(id)
            .name(name)
            .description(description)
            .startNodeId(startNodeId)
            .nodes(new ArrayList<>(nodes))
            .edges(edges)
            .build();
        workflowRepository.save(workflow);
    }

    private Node createDataNode(String id, String name, NodeType type, Map<String, Object> config) {
        return Node.builder()
            .id(id)
            .name(name)
            .type(type)
            .config(new HashMap<>(config))
            .inputMapping(new HashMap<>())
            .outputMapping(new HashMap<>())
            .build();
    }

    private Node createLlmNode(String id, String name, Map<String, Object> config) {
        return Node.builder()
            .id(id)
            .name(name)
            .type(NodeType.LLM_CALL)
            .config(new HashMap<>(config))
            .inputMapping(new HashMap<>())
            .outputMapping(new HashMap<>())
            .build();
    }

    private Node createConditionNode(String id, String name, List<Map<String, String>> conditions, String defaultNextNodeId) {
        List<Map<String, Object>> conditionMaps = new ArrayList<>();
        for (Map<String, String> cond : conditions) {
            conditionMaps.add(new HashMap<>(cond));
        }
        Map<String, Object> config = new HashMap<>();
        config.put("conditions", conditionMaps);
        config.put("defaultNextNodeId", defaultNextNodeId);
        return Node.builder()
            .id(id)
            .name(name)
            .type(NodeType.CONDITION)
            .config(config)
            .inputMapping(new HashMap<>())
            .outputMapping(new HashMap<>())
            .build();
    }

    private Node createBranchNode(String id, String name, List<Map<String, String>> branches, String defaultNextNodeId) {
        List<Map<String, Object>> branchMaps = new ArrayList<>();
        for (Map<String, String> branch : branches) {
            branchMaps.add(new HashMap<>(branch));
        }
        Map<String, Object> config = new HashMap<>();
        config.put("branches", branchMaps);
        config.put("defaultNextNodeId", defaultNextNodeId);
        return Node.builder()
            .id(id)
            .name(name)
            .type(NodeType.BRANCH)
            .config(config)
            .inputMapping(new HashMap<>())
            .outputMapping(new HashMap<>())
            .build();
    }

    private Node createParallelNode(String id, String name, List<Map<String, Object>> branches, String nextNodeId) {
        Map<String, Object> config = new HashMap<>();
        config.put("branches", branches);
        config.put("strategy", "AND");
        config.put("nextNodeId", nextNodeId);
        return Node.builder()
            .id(id)
            .name(name)
            .type(NodeType.PARALLEL)
            .config(config)
            .inputMapping(new HashMap<>())
            .outputMapping(new HashMap<>())
            .build();
    }

    private Node createForeachNode(String id, String name, String collection, String variableName, int maxIterations, String nextNodeId) {
        Map<String, Object> config = new HashMap<>();
        config.put("collection", collection);
        config.put("variableName", variableName);
        config.put("maxIterations", maxIterations);
        config.put("nextNodeId", nextNodeId);
        return Node.builder()
            .id(id)
            .name(name)
            .type(NodeType.FOREACH)
            .config(config)
            .inputMapping(new HashMap<>())
            .outputMapping(new HashMap<>())
            .build();
    }

    private Node createSubworkflowNode(String id, String name, String workflowId, String nextNodeId) {
        Map<String, Object> config = new HashMap<>();
        config.put("workflowId", workflowId);
        config.put("nextNodeId", nextNodeId);
        return Node.builder()
            .id(id)
            .name(name)
            .type(NodeType.SUBWORKFLOW)
            .config(config)
            .inputMapping(new HashMap<>())
            .outputMapping(new HashMap<>())
            .build();
    }

    private Node createStartNode(String id, String name, String triggerType,
                                String cron, String webhookPath,
                                Map<String, String> fieldMappings) {
        Map<String, Object> config = new HashMap<>();
        config.put("triggerType", triggerType);
        if (cron != null) config.put("cron", cron);
        if (webhookPath != null) config.put("webhookPath", webhookPath);
        if (fieldMappings != null) config.put("fieldMappings", new HashMap<>(fieldMappings));
        config.put("nextNodeId", null);

        return Node.builder()
            .id(id)
            .name(name)
            .type(NodeType.START)
            .config(config)
            .inputMapping(new HashMap<>())
            .outputMapping(new HashMap<>())
            .build();
    }

    private Node createEndNode(String id, String name) {
        Map<String, Object> config = new HashMap<>();
        config.put("nextNodeId", null);
        return Node.builder()
            .id(id)
            .name(name)
            .type(NodeType.END)
            .config(config)
            .inputMapping(new HashMap<>())
            .outputMapping(new HashMap<>())
            .build();
    }

    private Node createJoinNode(String id, String name) {
        Map<String, Object> config = new HashMap<>();
        config.put("nextNodeId", null);
        return Node.builder()
            .id(id)
            .name(name)
            .type(NodeType.JOIN)
            .config(config)
            .inputMapping(new HashMap<>())
            .outputMapping(new HashMap<>())
            .build();
    }

    private Edge createEdge(String id, String fromNodeId, String toNodeId) {
        return Edge.builder()
            .id(id)
            .fromNodeId(fromNodeId)
            .toNodeId(toNodeId)
            .build();
    }
}