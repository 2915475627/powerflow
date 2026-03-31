# 工作流触发器扩展设计

## 一、概述

为工作流系统新增 START 节点类型，支持定时任务（cron）和 Webhook 两种触发方式。工作流可配置"启用"开关，启用后根据 START 节点的触发器配置自动执行。

## 二、核心变更

### 2.1 新增 NodeType

```java
public enum NodeType {
    DATA_PROCESSING,
    CONDITION,
    HTTP_REQUEST,
    LLM_CALL,
    PARALLEL,
    FOREACH,
    BRANCH,
    SUBWORKFLOW,
    TRY_CATCH,
    RETRY,
    START  // 新增
}
```

### 2.2 START 节点配置

```json
{
  "id": "start_node",
  "name": "开始",
  "type": "START",
  "config": {
    "triggerType": "SCHEDULE | WEBHOOK | NONE",
    "cron": "0 9 * * *",           // triggerType=SCHEDULE 时必填
    "webhookPath": "/webhook/xxx",  // triggerType=WEBHOOK 时必填
    "fieldMappings": {               // Webhook payload 字段映射
      "userId": "input.userId",
      "data": "input.payload"
    }
  },
  "inputMapping": {},
  "outputMapping": {}
}
```

### 2.3 Workflow 实体变更

```java
public class Workflow {
    private final String id;
    private final String name;
    private final String description;
    private final Map<String, Node> nodes;
    private final List<Edge> edges;
    private final String startNodeId;
    private final boolean enabled;  // 新增：是否启用定时/webhook
}
```

### 2.4 新增 TriggerExecutionLog 实体

```java
public class TriggerExecutionLog {
    private String id;
    private String workflowId;
    private TriggerType triggerType;  // SCHEDULE, WEBHOOK, MANUAL
    private String triggerSource;      // cron表达式 或 webhook路径 或 "manual"
    private ExecutionStatus status;
    private String executionId;        // 关联的 workflow execution id
    private LocalDateTime triggeredAt;
    private String error;
}
```

## 三、服务设计

### 3.1 TriggerSchedulerService

- 启动时扫描所有 `enabled=true` 且 `startNode.triggerType=SCHEDULE` 的工作流
- 使用 `@Scheduled` + `CronTrigger` 执行调度
- 支持动态添加/移除调度任务（当工作流 enabled 状态变更时）

### 3.2 WebhookController

```java
@PostMapping("/webhook/{path}")
public ResponseEntity<?> triggerWebhook(
    @PathVariable String path,
    @RequestBody Map<String, Object> payload,
    @RequestHeader Map<String, String> headers) {

    // 1. 根据 path 查找对应工作流
    // 2. 应用 fieldMappings 到 payload，构建 Context
    // 3. 触发工作流执行
    // 4. 记录 TriggerExecutionLog
}
```

### 3.3 TriggerExecutionLogRepository

- `InMemoryTriggerExecutionLogRepository` 实现
- 按 workflowId 查询、按时间倒序

## 四、API 变更

### 4.1 更新工作流（启用/禁用）

```
PUT /api/workflows/{id}
Body: { ..., "enabled": true/false }
```

### 4.2 Webhook 触发端点

```
POST /webhook/{webhookPath}
Body: { webhook payload json }
```

### 4.3 触发历史查询

```
GET /api/trigger-logs?workflowId={workflowId}&limit=50
```

### 4.4 手动触发测试

```
POST /api/workflows/{id}/execute (已有)
Body: { ...context }
```

## 五、前端变更

### 5.1 工作流配置界面

- 原有工作流配置界面基础上加"启用"开关（toggle）
- 启用开关仅在 `startNode.type == START` 且 `startNode.triggerType != NONE` 时可编辑
- 非触发器工作流（无 START 节点或 triggerType==NONE）禁用开关

### 5.2 START 节点配置面板

- 新增"触发器配置"区块
- 触发类型下拉：None / 定时任务 / Webhook
- 定时任务：显示 cron 输入框
- Webhook：显示 webhook 路径输入框 + 字段映射表格

## 六、执行流程

### 6.1 定时任务触发流程

```
1. TriggerSchedulerService 扫描 enabled=true 的工作流
2. 根据 START 节点 cron 表达式注册调度任务
3. cron 时间到达 → 创建 Context(Map.empty) → 执行工作流
4. 记录 TriggerExecutionLog(status=SUCCESS/FAILED)
```

### 6.2 Webhook 触发流程

```
1. 外部系统 POST /webhook/{path}
2. WebhookController 根据 path 查找工作流
3. 解析 payload，应用 fieldMappings 构建 Context
4. 执行工作流
5. 记录 TriggerExecutionLog
```

### 6.3 手动触发测试

```
1. 用户点击"测试执行"按钮
2. 构建 Context（用户输入或默认空）
3. 执行工作流（与现有逻辑相同）
4. 不记录 TriggerExecutionLog（标记为 MANUAL）
```

## 七、边界情况

| 场景 | 处理方式 |
|------|---------|
| cron 表达式格式错误 | 验证时拒绝保存，工作流不注册调度 |
| webhook path 重复 | 验证时拒绝保存 |
| 工作流执行失败 | TriggerExecutionLog 记录 FAILED + error 信息 |
| 调度时工作流不存在 | 跳过调度，输出警告日志 |
| webhook payload 缺少映射字段 | 字段留空，不影响执行 |

## 八、技术选型

- 调度器：Spring `@Scheduled` + `CronTrigger`（内存实现）
- Webhook：Spring MVC Controller
- 前端：React + 复用现有组件模式

## 九、验证检查清单

- [ ] START 节点可正常添加到工作流
- [ ] 定时任务工作流可启用/禁用
- [ ] Webhook 工作流可启用/禁用
- [ ] cron 表达式正确时定时触发
- [ ] cron 表达式错误时拒绝保存
- [ ] Webhook 正确触发工作流
- [ ] 字段映射正确传递 payload 数据
- [ ] 触发历史可查询
- [ ] 手动执行测试正常
- [ ] 非触发器工作流不能启用
