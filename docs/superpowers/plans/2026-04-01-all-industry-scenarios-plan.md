# 全行业场景测试实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为6大行业实现25个业务场景测试，验证工作流引擎全部10种节点类型

**Architecture:** 采用行业垂直划分，每个行业独立测试文件，继承公共测试基类减少样板代码

**Tech Stack:** Java 17, JUnit 5, Spring Boot 3.2, SpEL

---

## 文件结构

```
backend/src/test/java/com/powerflow/workflow/integration/
├── EcommerceBusinessScenariosTest.java    (已有，电商5场景)
├── FinanceBusinessScenariosTest.java      (新建，金融4场景)
├── EnterpriseBusinessScenariosTest.java   (新建，企业办公4场景)
├── SocialContentScenariosTest.java       (新建，社交内容4场景)
├── IoTBusinessScenariosTest.java         (新建，物联网4场景)
└── PublicServiceScenariosTest.java       (新建，公共服务4场景)
```

**关键文件参考：**
- `backend/src/test/java/com/powerflow/workflow/integration/EcommerceBusinessScenariosTest.java` - 现有模式
- `backend/src/main/java/com/powerflow/workflow/domain/model/enums/NodeType.java` - 节点类型定义
- `backend/src/main/java/com/powerflow/workflow/domain/service/handler/*.java` - 各Handler配置

---

## Task 1: 电商场景测试 (5场景)

**Files:**
- Modify: `backend/src/test/java/com/powerflow/workflow/integration/EcommerceBusinessScenariosTest.java`

- [ ] **Step 1: 添加智能客服路由测试 (BRANCH + LLM)**

```java
@Test
@DisplayName("Should route to correct department based on LLM intent")
void should_route_based_on_intent() {
    // LLM节点返回intent=search
    Node intentNode = Node.builder()
        .id("intent")
        .name("Intent Recognition")
        .type(NodeType.LLM_CALL)
        .config(Map.of(
            "prompt", "Extract intent from: #input.query",
            "outputKey", "intent",
            "temperature", 0.2
        ))
        .inputMapping(Map.of("query", "input.query"))
        .outputMapping(Map.of("intent", "intent"))
        .build();

    // BRANCH节点路由
    Node branch = Node.builder()
        .id("route")
        .name("Route")
        .type(NodeType.BRANCH)
        .config(Map.of("branches", List.of(
            Map.of("name", "search", "expression", "#input.intent == 'search'", "nextNodeId", "search_node"),
            Map.of("name", "recommend", "expression", "#input.intent == 'recommend'", "nextNodeId", "recommend_node"),
            Map.of("name", "inquiry", "expression", "#input.intent == 'inquiry'", "nextNodeId", "inquiry_node"),
            Map.of("name", "order", "expression", "#input.intent == 'order'", "nextNodeId", "order_node")
        )))
        .inputMapping(Map.of("intent", "intent"))
        .outputMapping(Map.of())
        .build();

    // 4个路由目标节点
    Node searchNode = Node.builder()...
    Node recommendNode = Node.builder()...
    Node inquiryNode = Node.builder()...
    Node orderNode = Node.builder()...

    // 构建工作流，验证路由正确
}
```

- [ ] **Step 2: 添加并行推荐测试 (PARALLEL)**

```java
@Test
@DisplayName("Should fetch user profile and behaviors in parallel")
void should_fetch_profile_and_behaviors_in_parallel() {
    Node parallelNode = Node.builder()
        .id("parallel")
        .name("Parallel Fetch")
        .type(NodeType.PARALLEL)
        .config(Map.of(
            "branches", List.of(
                Map.of("name", "profile", "nodeIds", List.of("profile_node")),
                Map.of("name", "behaviors", "nodeIds", List.of("behaviors_node"))
            ),
            "strategy", "AND",
            "nextNodeId", "merge"
        ))
        .inputMapping(Map.of())
        .outputMapping(Map.of())
        .build();
    // 验证并行执行
}
```

- [ ] **Step 3: 添加FOREACH批量处理测试**

```java
@Test
@DisplayName("Should iterate over product list")
void should_iterate_over_products() {
    Node foreachNode = Node.builder()
        .id("foreach")
        .name("Process Products")
        .type(NodeType.FOREACH)
        .config(Map.of(
            "collection", "#input.products",
            "variableName", "product",
            "maxIterations", 100,
            "nextNodeId", "aggregate"
        ))
        .inputMapping(Map.of("products", "input.products"))
        .outputMapping(Map.of("results", "processed"))
        .build();
}
```

- [ ] **Step 4: 添加TRY_CATCH异常处理测试**

```java
@Test
@DisplayName("Should catch exception in order creation")
void should_catch_order_exception() {
    Node tryCatch = Node.builder()
        .id("try_catch")
        .name("Try Create Order")
        .type(NodeType.TRY_CATCH)
        .config(Map.of(
            "tryNodeId", "create_order",
            "catchNodeId", "handle_error",
            "nextNodeId", null
        ))
        .inputMapping(Map.of())
        .outputMapping(Map.of())
        .build();
}
```

- [ ] **Step 5: 运行测试验证**

Run: `cd backend && mvn test -Dtest=EcommerceBusinessScenariosTest -q`
Expected: 9 tests pass (原有4 + 新增5)

- [ ] **Step 6: 提交**

```bash
git add backend/src/test/java/com/powerflow/workflow/integration/EcommerceBusinessScenariosTest.java
git commit -m "test(ecommerce): add 5 business scenario tests"
```

---

## Task 2: 金融场景测试 (4场景)

**Files:**
- Create: `backend/src/test/java/com/powerflow/workflow/integration/FinanceBusinessScenariosTest.java`

- [ ] **Step 1: 创建金融测试文件，继承电商测试模式**

```java
package com.powerflow.workflow.integration;

import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.model.enums.NodeType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class FinanceBusinessScenariosTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("风控评估场景")
    class RiskAssessmentScenario {
        @Test
        void should_assess_risk_and_decide() {
            // 1. 获取征信数据 (HTTP)
            // 2. LLM风控评分
            // 3. CONDITION决策: score>=0.7通过 / <0.7拒绝
            // 4. HTTP通知结果
        }
    }

    @Nested
    @DisplayName("贷款审批场景")
    class LoanApprovalScenario {
        @Test
        void should_handle_loan_approval_with_try_catch() {
            // 1. DATA_INPUT接收申请
            // 2. HTTP验证材料
            // 3. LLM信用评估
            // 4. CONDITION金额判断
            // 5. TRY_CATCH异常处理
        }
    }

    @Nested
    @DisplayName("反欺诈检测场景")
    class AntiFraudScenario {
        @Test
        void should_detect_fraud_in_parallel() {
            // 1. HTTP获取交易
            // 2. PARALLEL并行检测(金额/频率/位置/设备)
            // 3. CONDITION任一异常则拦截
        }
    }

    @Nested
    @DisplayName("账户审核场景")
    class AccountAuditScenario {
        @Test
        void should_audit_account_with_subworkflow() {
            // 1. LLM提取信息
            // 2. CONDITION基础检查
            // 3. SUBWORKFLOW深度审核
        }
    }
}
```

- [ ] **Step 2: 实现风控评估测试**

```java
@Test
void should_assess_risk_and_decide() {
    // HTTP获取征信
    Node fetchCredit = Node.builder()
        .id("fetch_credit")
        .type(NodeType.HTTP_REQUEST)
        .config(Map.of(
            "url", "https://api.credit.com/score",
            "method", "GET",
            "outputKey", "creditScore"
        ))
        .inputMapping(Map.of())
        .outputMapping(Map.of("creditScore", "creditScore"))
        .build();

    // LLM风控评分
    Node riskScore = Node.builder()
        .id("risk_score")
        .type(NodeType.LLM_CALL)
        .config(Map.of(
            "prompt", "基于征信 #{input.creditScore} 评估风险",
            "outputKey", "riskLevel",
            "temperature", 0.3
        ))
        .inputMapping(Map.of("creditScore", "creditScore"))
        .outputMapping(Map.of("riskLevel", "riskLevel"))
        .build();

    // CONDITION决策
    Node decision = Node.builder()
        .id("decision")
        .type(NodeType.CONDITION)
        .config(Map.of(
            "conditions", List.of(
                Map.of("expression", "#input.riskLevel >= 0.7", "nextNodeId", "approve"),
                Map.of("expression", "#input.riskLevel < 0.7", "nextNodeId", "reject")
            ),
            "defaultNextNodeId", "reject"
        ))
        .inputMapping(Map.of("riskLevel", "riskLevel"))
        .outputMapping(Map.of())
        .build();

    // 验证结果
}
```

- [ ] **Step 3: 实现贷款审批测试 (TRY_CATCH)**

```java
@Test
void should_handle_loan_approval_with_try_catch() {
    Node tryCatch = Node.builder()
        .id("try_catch")
        .type(NodeType.TRY_CATCH)
        .config(Map.of(
            "tryNodeId", "create_order",
            "catchNodeId", "rollback",
            "nextNodeId", "notify"
        ))
        .inputMapping(Map.of())
        .outputMapping(Map.of())
        .build();
}
```

- [ ] **Step 4: 实现反欺诈检测测试 (PARALLEL)**

```java
@Test
void should_detect_fraud_in_parallel() {
    Node parallel = Node.builder()
        .id("parallel_check")
        .type(NodeType.PARALLEL)
        .config(Map.of(
            "branches", List.of(
                Map.of("name", "amount", "nodeIds", List.of("check_amount")),
                Map.of("name", "frequency", "nodeIds", List.of("check_frequency")),
                Map.of("name", "location", "nodeIds", List.of("check_location")),
                Map.of("name", "device", "nodeIds", List.of("check_device"))
            ),
            "strategy", "AND"
        ))
        .inputMapping(Map.of())
        .outputMapping(Map.of())
        .build();
}
```

- [ ] **Step 5: 实现账户审核测试 (SUBWORKFLOW)**

```java
@Test
void should_audit_account_with_subworkflow() {
    Node subworkflow = Node.builder()
        .id("deep_audit")
        .type(NodeType.SUBWORKFLOW)
        .config(Map.of(
            "workflowId", "account-audit-subwf",
            "nextNodeId", "notify"
        ))
        .inputMapping(Map.of())
        .outputMapping(Map.of())
        .build();
}
```

- [ ] **Step 6: 运行测试验证**

Run: `cd backend && mvn test -Dtest=FinanceBusinessScenariosTest -q`
Expected: 4 tests pass

- [ ] **Step 7: 提交**

```bash
git add backend/src/test/java/com/powerflow/workflow/integration/FinanceBusinessScenariosTest.java
git commit -m "test(finance): add 4 business scenario tests"
```

---

## Task 3: 企业办公场景测试 (4场景)

**Files:**
- Create: `backend/src/test/java/com/powerflow/workflow/integration/EnterpriseBusinessScenariosTest.java`

- [ ] **Step 1: 创建企业办公测试文件**

```java
class EnterpriseBusinessScenariosTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("审批流场景")
    class ApprovalFlowScenario {
        @Test
        void should_handle_multi_level_approval() {
            // 1. DATA_INPUT接收申请
            // 2. CONDITION加急判断
            // 3. HTTP一级审批
            // 4. SUBWORKFLOW二级审批
        }
    }

    @Nested
    @DisplayName("数据报表场景")
    class ReportGenerationScenario {
        @Test
        void should_generate_report_with_parallel_fetch_and_foreach() {
            // 1. HTTP×3并行采集(销售/库存/用户)
            // 2. FOREACH遍历处理
            // 3. DATA_PROCESSING汇总
            // 4. LLM生成摘要
        }
    }

    @Nested
    @DisplayName("任务分发场景")
    class TaskDispatchScenario {
        @Test
        void should_dispatch_tasks_in_foreach_loop() {
            // 1. DATA_INPUT获取任务列表
            // 2. FOREACH遍历
            // 3. HTTP分发单个任务
        }
    }

    @Nested
    @DisplayName("会议安排场景")
    class MeetingScheduleScenario {
        @Test
        void should_schedule_meeting_with_llm_and_condition() {
            // 1. LLM解析会议请求
            // 2. CONDITION检查日程
            // 3. HTTP预定会议室或通知冲突
        }
    }
}
```

- [ ] **Step 2: 实现审批流测试 (SUBWORKFLOW嵌套)**

```java
@Test
void should_handle_multi_level_approval() {
    Node level2Subworkflow = Node.builder()
        .id("level2")
        .type(NodeType.SUBWORKFLOW)
        .config(Map.of(
            "workflowId", "approval-level2-wf",
            "nextNodeId", "notify"
        ))
        .inputMapping(Map.of())
        .outputMapping(Map.of())
        .build();
}
```

- [ ] **Step 3: 实现数据报表测试 (HTTP并行 + FOREACH)**

```java
@Test
void should_generate_report_with_parallel_fetch_and_foreach() {
    Node parallelFetch = Node.builder()
        .id("parallel_fetch")
        .type(NodeType.PARALLEL)
        .config(Map.of(
            "branches", List.of(
                Map.of("name", "sales", "nodeIds", List.of("sales_node")),
                Map.of("name", "inventory", "nodeIds", List.of("inventory_node")),
                Map.of("name", "users", "nodeIds", List.of("users_node"))
            ),
            "strategy", "AND"
        ))
        .inputMapping(Map.of())
        .outputMapping(Map.of())
        .build();

    Node foreachProcess = Node.builder()
        .id("foreach_process")
        .type(NodeType.FOREACH)
        .config(Map.of(
            "collection", "#input.dataItems",
            "variableName", "item",
            "maxIterations", 1000
        ))
        .inputMapping(Map.of())
        .outputMapping(Map.of("results", "processedItems"))
        .build();
}
```

- [ ] **Step 4: 实现任务分发测试 (FOREACH循环)**

```java
@Test
void should_dispatch_tasks_in_foreach_loop() {
    Node foreachDispatch = Node.builder()
        .id("foreach_dispatch")
        .type(NodeType.FOREACH)
        .config(Map.of(
            "collection", "#input.taskList",
            "variableName", "task",
            "maxIterations", 50,
            "nextNodeId", "summary"
        ))
        .inputMapping(Map.of("taskList", "input.taskList"))
        .outputMapping(Map.of("results", "dispatchResults"))
        .build();
}
```

- [ ] **Step 5: 运行测试验证**

Run: `cd backend && mvn test -Dtest=EnterpriseBusinessScenariosTest -q`
Expected: 4 tests pass

- [ ] **Step 6: 提交**

```bash
git add backend/src/test/java/com/powerflow/workflow/integration/EnterpriseBusinessScenariosTest.java
git commit -m "test(enterprise): add 4 business scenario tests"
```

---

## Task 4: 社交内容场景测试 (4场景)

**Files:**
- Create: `backend/src/test/java/com/powerflow/workflow/integration/SocialContentScenariosTest.java`

- [ ] **Step 1: 创建社交内容测试文件**

```java
class SocialContentScenariosTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("内容审核场景")
    class ContentModerationScenario {
        @Test
        void should_moderate_content_with_condition() {
            // 1. HTTP获取内容
            // 2. LLM审核判断
            // 3. CONDITION三级分类(发布/人工/拦截)
        }
    }

    @Nested
    @DisplayName("用户画像场景")
    class UserProfileScenario {
        @Test
        void should_build_user_profile_in_parallel() {
            // 1. HTTP×4并行(基本信息/行为/兴趣/社交)
            // 2. DATA_PROCESSING合并
            // 3. LLM生成描述
        }
    }

    @Nested
    @DisplayName("推荐分发场景")
    class RecommendationDistributionScenario {
        @Test
        void should_distribute_to_multiple_channels() {
            // 1. HTTP获取推荐
            // 2. LLM生成列表
            // 3. PARALLEL分发到各渠道
        }
    }

    @Nested
    @DisplayName("舆情分析场景")
    class SentimentAnalysisScenario {
        @Test
        void should_analyze_sentiment_in_parallel() {
            // 1. HTTP采集内容
            // 2. LLM×3并行(情感/主题/热度)
            // 3. DATA_PROCESSING汇总
        }
    }
}
```

- [ ] **Step 2: 实现内容审核测试 (CONDITION三级)**

```java
@Test
void should_moderate_content_with_condition() {
    Node decision = Node.builder()
        .id("decision")
        .type(NodeType.CONDITION)
        .config(Map.of(
            "conditions", List.of(
                Map.of("expression", "#input.auditResult == 'pass'", "nextNodeId", "publish"),
                Map.of("expression", "#input.auditResult == 'review'", "nextNodeId", "manual_review"),
                Map.of("expression", "#input.auditResult == 'block'", "nextNodeId", "block")
            ),
            "defaultNextNodeId", "manual_review"
        ))
        .inputMapping(Map.of("auditResult", "auditResult"))
        .outputMapping(Map.of())
        .build();
}
```

- [ ] **Step 3: 实现用户画像测试 (HTTP×4并行)**

```java
@Test
void should_build_user_profile_in_parallel() {
    Node parallelFetch = Node.builder()
        .id("parallel")
        .type(NodeType.PARALLEL)
        .config(Map.of(
            "branches", List.of(
                Map.of("name", "basic", "nodeIds", List.of("basic_node")),
                Map.of("name", "behavior", "nodeIds", List.of("behavior_node")),
                Map.of("name", "interest", "nodeIds", List.of("interest_node")),
                Map.of("name", "social", "nodeIds", List.of("social_node"))
            ),
            "strategy", "AND"
        ))
        .inputMapping(Map.of())
        .outputMapping(Map.of())
        .build();
}
```

- [ ] **Step 4: 运行测试验证**

Run: `cd backend && mvn test -Dtest=SocialContentScenariosTest -q`
Expected: 4 tests pass

- [ ] **Step 5: 提交**

```bash
git add backend/src/test/java/com/powerflow/workflow/integration/SocialContentScenariosTest.java
git commit -m "test(social): add 4 business scenario tests"
```

---

## Task 5: 物联网IoT场景测试 (4场景)

**Files:**
- Create: `backend/src/test/java/com/powerflow/workflow/integration/IoTBusinessScenariosTest.java`

- [ ] **Step 1: 创建IoT测试文件**

```java
class IoTBusinessScenariosTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("设备管控场景")
    class DeviceControlScenario {
        @Test
        void should_control_devices_in_foreach_loop() {
            // 1. DATA_INPUT获取指令列表
            // 2. FOREACH遍历设备
            // 3. HTTP发送指令
            // 4. CONDITION检查结果
        }
    }

    @Nested
    @DisplayName("数据采集场景")
    class DataCollectionScenario {
        @Test
        void should_collect_data_with_threshold_alert() {
            // 1. HTTP×N并行采集
            // 2. DATA_PROCESSING处理
            // 3. CONDITION阈值判断
        }
    }

    @Nested
    @DisplayName("告警触发场景")
    class AlertTriggerScenario {
        @Test
        void should_trigger_multi_level_alerts() {
            // 1. HTTP接收告警
            // 2. CONDITION判断级别
            // 3. HTTP×3通知(相关/主管/紧急)
            // 4. SUBWORKFLOW自动处理
        }
    }

    @Nested
    @DisplayName("远程诊断场景")
    class RemoteDiagnosisScenario {
        @Test
        void should_diagnose_with_llm_and_condition() {
            // 1. LLM解析症状
            // 2. HTTP获取诊断历史
            // 3. LLM诊断建议
            // 4. CONDITION是否有方案
        }
    }
}
```

- [ ] **Step 2: 实现设备管控测试 (FOREACH + CONDITION)**

```java
@Test
void should_control_devices_in_foreach_loop() {
    Node foreachDevice = Node.builder()
        .id("foreach_device")
        .type(NodeType.FOREACH)
        .config(Map.of(
            "collection", "#input.deviceList",
            "variableName", "device",
            "maxIterations", 100,
            "nextNodeId", "check_result"
        ))
        .inputMapping(Map.of("deviceList", "input.deviceList"))
        .outputMapping(Map.of("results", "commandResults"))
        .build();

    Node checkResult = Node.builder()
        .id("check_result")
        .type(NodeType.CONDITION)
        .config(Map.of(
            "conditions", List.of(
                Map.of("expression", "#input.success == true", "nextNodeId", "next"),
                Map.of("expression", "#input.success == false", "nextNodeId", "retry")
            ),
            "defaultNextNodeId", "retry"
        ))
        .inputMapping(Map.of())
        .outputMapping(Map.of())
        .build();
}
```

- [ ] **Step 3: 实现告警触发测试 (CONDITION + SUBWORKFLOW)**

```java
@Test
void should_trigger_multi_level_alerts() {
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
        .inputMapping(Map.of())
        .outputMapping(Map.of())
        .build();
}
```

- [ ] **Step 4: 运行测试验证**

Run: `cd backend && mvn test -Dtest=IoTBusinessScenariosTest -q`
Expected: 4 tests pass

- [ ] **Step 5: 提交**

```bash
git add backend/src/test/java/com/powerflow/workflow/integration/IoTBusinessScenariosTest.java
git commit -m "test(iot): add 4 business scenario tests"
```

---

## Task 6: 公共服务场景测试 (4场景)

**Files:**
- Create: `backend/src/test/java/com/powerflow/workflow/integration/PublicServiceScenariosTest.java`

- [ ] **Step 1: 创建公共服务测试文件**

```java
class PublicServiceScenariosTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("表单处理场景")
    class FormProcessingScenario {
        @Test
        void should_process_form_with_intelligent_routing() {
            // 1. DATA_INPUT接收表单
            // 2. DATA_PROCESSING验证
            // 3. CONDITION业务类型分流
            // 4. HTTP×2处理
        }
    }

    @Nested
    @DisplayName("资质审核场景")
    class QualificationAuditScenario {
        @Test
        void should_audit_qualification_with_subworkflow() {
            // 1. HTTP获取材料
            // 2. LLM预审
            // 3. CONDITION通过/详细审核
            // 4. SUBWORKFLOW深度审核
        }
    }

    @Nested
    @DisplayName("进度查询场景")
    class ProgressQueryScenario {
        @Test
        void should_query_progress_with_llm_response() {
            // 1. DATA_INPUT接收查询
            // 2. HTTP查询状态
            // 3. DATA_PROCESSING格式化
            // 4. LLM生成回复
        }
    }

    @Nested
    @DisplayName("投诉处理场景")
    class ComplaintHandlingScenario {
        @Test
        void should_handle_complaint_with_classification() {
            // 1. DATA_INPUT接收投诉
            // 2. LLM分类
            // 3. CONDITION紧急程度
            // 4. HTTP派发
            // 5. SUBWORKFLOW跟踪
        }
    }
}
```

- [ ] **Step 2: 实现进度查询测试 (HTTP + LLM生成回复)**

```java
@Test
void should_query_progress_with_llm_response() {
    Node getStatus = Node.builder()
        .id("get_status")
        .type(NodeType.HTTP_REQUEST)
        .config(Map.of(
            "url", "https://api.gov.com/progress",
            "method", "GET",
            "outputKey", "statusData"
        ))
        .inputMapping(Map.of("caseId", "input.caseId"))
        .outputMapping(Map.of("statusData", "status"))
        .build();

    Node formatStatus = Node.builder()
        .id("format_status")
        .type(NodeType.DATA_PROCESSING)
        .config(Map.of(
            "outputKey", "formattedStatus",
            "expression", "'当前状态: ' + #input.status + ', 预计完成: ' + #input.eta"
        ))
        .inputMapping(Map.of("status", "status", "eta", "eta"))
        .outputMapping(Map.of("formattedStatus", "reply"))
        .build();

    Node generateReply = Node.builder()
        .id("generate_reply")
        .type(NodeType.LLM_CALL)
        .config(Map.of(
            "prompt", "将以下进度信息转换为友好回复: #input.formattedStatus",
            "outputKey", "reply"
        ))
        .inputMapping(Map.of("formattedStatus", "reply"))
        .outputMapping(Map.of("reply", "output.reply"))
        .build();
}
```

- [ ] **Step 3: 实现投诉处理测试 (LLM分类 + SUBWORKFLOW)**

```java
@Test
void should_handle_complaint_with_classification() {
    Node classify = Node.builder()
        .id("classify")
        .type(NodeType.LLM_CALL)
        .config(Map.of(
            "prompt", "分析投诉内容，分类为: 质量/服务/物流/其他",
            "outputKey", "category"
        ))
        .inputMapping(Map.of("content", "input.content"))
        .outputMapping(Map.of("category", "category"))
        .build();

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
        .inputMapping(Map.of())
        .outputMapping(Map.of())
        .build();
}
```

- [ ] **Step 4: 运行测试验证**

Run: `cd backend && mvn test -Dtest=PublicServiceScenariosTest -q`
Expected: 4 tests pass

- [ ] **Step 5: 提交**

```bash
git add backend/src/test/java/com/powerflow/workflow/integration/PublicServiceScenariosTest.java
git commit -m "test(public): add 4 business scenario tests"
```

---

## Task 7: 最终验证和汇总

- [ ] **Step 1: 运行全部测试**

Run: `cd backend && mvn test -q`
Expected: 26原有 + 25新增 = 51 tests pass

- [ ] **Step 2: 生成测试报告**

Run: `cd backend && mvn test -DgenerateReports=true`
Verify: 控制台输出所有场景测试通过

- [ ] **Step 3: 推送分支**

```bash
git checkout -b subproject-7-all-industry-scenarios
git push -u origin subproject-7-all-industry-scenarios
```

---

## 验证检查清单

| 节点类型 | 电商 | 金融 | 企业 | 社交 | IoT | 公共服务 | 覆盖 |
|----------|------|------|------|------|-----|---------|------|
| DATA_INPUT | ✓ | ✓ | ✓ | | ✓ | ✓ | 5 |
| DATA_PROCESSING | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | 6 |
| LLM_CALL | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | 6 |
| HTTP_REQUEST | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | 6 |
| CONDITION | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | 6 |
| BRANCH | ✓ | | | | | | 1 |
| PARALLEL | ✓ | ✓ | ✓ | ✓ | ✓ | | 5 |
| FOREACH | ✓ | | ✓ | | ✓ | | 3 |
| SUBWORKFLOW | | ✓ | ✓ | | ✓ | ✓ | 4 |
| TRY_CATCH | ✓ | ✓ | | | | | 2 |

---

**Plan complete.** Saved to `docs/superpowers/plans/2026-04-01-all-industry-scenarios-plan.md`

**Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
