# 工作流触发器扩展实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 START 节点类型，支持定时任务（cron）和 Webhook 触发，扩展工作流启用/禁用管理

**Architecture:** 后端新增 TriggerSchedulerService 调度服务和 WebhookController，在 NodeType 新增 START 类型，Workflow 增加 enabled 字段。前端复用现有工作流配置界面，START 节点配置面板增加触发器配置区块。

**Tech Stack:** Spring Boot @Scheduled + CronTrigger, React, TypeScript

---

## 文件结构

**后端新增:**
- `TriggerType.java` - 触发类型枚举
- `TriggerExecutionLog.java` - 触发日志实体
- `TriggerExecutionLogRepository.java` - 触发日志仓库接口
- `InMemoryTriggerExecutionLogRepository.java` - 内存实现
- `TriggerSchedulerService.java` - 调度服务

**后端修改:**
- `NodeType.java` - 新增 START
- `Workflow.java` - 新增 enabled 字段
- `InMemoryWorkflowRepository.java` - 支持 enabled 查询
- `WorkflowExecutor.java` - 支持手动触发标记
- `WorkflowController.java` - 新增 trigger-logs 端点
- `WorkflowDataInitializer.java` - 添加 START 节点示例

**前端新增:**
- `TriggerConfig.tsx` - 触发器配置组件
- `TriggerLogsPage.tsx` - 触发历史页面

**前端修改:**
- `NodeTypes.ts` - 新增 START 类型
- `NodeConfigPanel.tsx` - START 节点配置面板
- `WorkflowEditor.tsx` - 启用开关
- `workflowsApi.ts` - 更新类型定义

---

## Task 1: 后端 - 新增 TriggerType 枚举

**Files:**
- Create: `backend/src/main/java/com/powerflow/workflow/domain/model/enums/TriggerType.java`

- [ ] **Step 1: 创建 TriggerType 枚举**

```java
package com.powerflow.workflow.domain.model.enums;

public enum TriggerType {
    NONE,
    SCHEDULE,
    WEBHOOK
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/powerflow/workflow/domain/model/enums/TriggerType.java
git commit -m "feat: add TriggerType enum"
```

---

## Task 2: 后端 - 新增 TriggerExecutionLog 实体

**Files:**
- Create: `backend/src/main/java/com/powerflow/workflow/domain/model/TriggerExecutionLog.java`

- [ ] **Step 1: 创建 TriggerExecutionLog 实体**

```java
package com.powerflow.workflow.domain.model;

import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import com.powerflow.workflow.domain.model.enums.TriggerType;
import java.time.LocalDateTime;

public class TriggerExecutionLog {
    private String id;
    private String workflowId;
    private TriggerType triggerType;
    private String triggerSource;
    private ExecutionStatus status;
    private String executionId;
    private LocalDateTime triggeredAt;
    private String error;

    public TriggerExecutionLog(String id, String workflowId, TriggerType triggerType,
                               String triggerSource, ExecutionStatus status,
                               String executionId, LocalDateTime triggeredAt, String error) {
        this.id = id;
        this.workflowId = workflowId;
        this.triggerType = triggerType;
        this.triggerSource = triggerSource;
        this.status = status;
        this.executionId = executionId;
        this.triggeredAt = triggeredAt;
        this.error = error;
    }

    // Getters
    public String getId() { return id; }
    public String getWorkflowId() { return workflowId; }
    public TriggerType getTriggerType() { return triggerType; }
    public String getTriggerSource() { return triggerSource; }
    public ExecutionStatus getStatus() { return status; }
    public String getExecutionId() { return executionId; }
    public LocalDateTime getTriggeredAt() { return triggeredAt; }
    public String getError() { return error; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String workflowId;
        private TriggerType triggerType;
        private String triggerSource;
        private ExecutionStatus status;
        private String executionId;
        private LocalDateTime triggeredAt;
        private String error;

        public Builder id(String id) { this.id = id; return this; }
        public Builder workflowId(String workflowId) { this.workflowId = workflowId; return this; }
        public Builder triggerType(TriggerType triggerType) { this.triggerType = triggerType; return this; }
        public Builder triggerSource(String triggerSource) { this.triggerSource = triggerSource; return this; }
        public Builder status(ExecutionStatus status) { this.status = status; return this; }
        public Builder executionId(String executionId) { this.executionId = executionId; return this; }
        public Builder triggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; return this; }
        public Builder error(String error) { this.error = error; return this; }

        public TriggerExecutionLog build() {
            return new TriggerExecutionLog(id, workflowId, triggerType, triggerSource,
                                         status, executionId, triggeredAt, error);
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/powerflow/workflow/domain/model/TriggerExecutionLog.java
git commit -m "feat: add TriggerExecutionLog entity"
```

---

## Task 3: 后端 - 新增 TriggerExecutionLogRepository 接口

**Files:**
- Create: `backend/src/main/java/com/powerflow/workflow/domain/port/outbound/TriggerExecutionLogRepository.java`

- [ ] **Step 1: 创建接口**

```java
package com.powerflow.workflow.domain.port.outbound;

import com.powerflow.workflow.domain.model.TriggerExecutionLog;
import java.util.List;

public interface TriggerExecutionLogRepository {
    void save(TriggerExecutionLog log);
    List<TriggerExecutionLog> findByWorkflowId(String workflowId, int limit);
    List<TriggerExecutionLog> findAll(int limit);
}
```

- [ ] **Step 2: 创建内存实现**

- Create: `backend/src/main/java/com/powerflow/workflow/adapter/outbound/persistence/InMemoryTriggerExecutionLogRepository.java`

```java
package com.powerflow.workflow.adapter.outbound.persistence;

import com.powerflow.workflow.domain.model.TriggerExecutionLog;
import com.powerflow.workflow.domain.port.outbound.TriggerExecutionLogRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class InMemoryTriggerExecutionLogRepository implements TriggerExecutionLogRepository {

    private final Map<String, List<TriggerExecutionLog>> logsByWorkflow = new ConcurrentHashMap<>();
    private final List<TriggerExecutionLog> allLogs = new ArrayList<>();

    @Override
    public void save(TriggerExecutionLog log) {
        logsByWorkflow.computeIfAbsent(log.getWorkflowId(), k -> new ArrayList<>()).add(log);
        allLogs.add(log);
    }

    @Override
    public List<TriggerExecutionLog> findByWorkflowId(String workflowId, int limit) {
        return logsByWorkflow.getOrDefault(workflowId, List.of()).stream()
            .sorted((a, b) -> b.getTriggeredAt().compareTo(a.getTriggeredAt()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public List<TriggerExecutionLog> findAll(int limit) {
        return allLogs.stream()
            .sorted((a, b) -> b.getTriggeredAt().compareTo(a.getTriggeredAt()))
            .limit(limit)
            .collect(Collectors.toList());
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/powerflow/workflow/domain/port/outbound/TriggerExecutionLogRepository.java
git add backend/src/main/java/com/powerflow/workflow/adapter/outbound/persistence/InMemoryTriggerExecutionLogRepository.java
git commit -m "feat: add TriggerExecutionLogRepository interface and in-memory implementation"
```

---

## Task 4: 后端 - NodeType 新增 START

**Files:**
- Modify: `backend/src/main/java/com/powerflow/workflow/domain/model/enums/NodeType.java:1-15`

- [ ] **Step 1: 修改 NodeType 枚举**

```java
package com.powerflow.workflow.domain.model.enums;

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
    START
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/powerflow/workflow/domain/model/enums/NodeType.java
git commit -m "feat: add START node type"
```

---

## Task 5: 后端 - Workflow 新增 enabled 字段

**Files:**
- Modify: `backend/src/main/java/com/powerflow/workflow/domain/model/Workflow.java:1-89`

- [ ] **Step 1: 修改 Workflow 类**

需要修改的位置：
- 添加 `enabled` 字段 (line ~19)
- 修改构造函数和 Builder (line ~39)
- 添加 `isEnabled()` getter (line ~53)

```java
// 在现有字段后添加
private final boolean enabled;

// 在构造函数中添加 (JsonCreator)
@JsonProperty("enabled") boolean enabled

// 在 Builder 添加
private boolean enabled;

// 在 Builder.build() 之前添加
public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }

// 添加 getter
public boolean isEnabled() { return enabled; }
```

完整修改后的关键部分：

```java
private final String id;
private final String name;
private final String description;
private final Map<String, Node> nodes;
private final List<Edge> edges;
private final String startNodeId;
private final boolean enabled;  // 新增

@JsonCreator
public Workflow(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("nodes") Map<String, Node> nodeMap,
        @JsonProperty("edges") List<Edge> edges,
        @JsonProperty("startNodeId") String startNodeId,
        @JsonProperty("enabled") Boolean enabled) {  // 修改这里
    this.id = id;
    this.name = name;
    this.description = description;
    this.nodes = nodeMap != null ? new HashMap<>(nodeMap) : new HashMap<>();
    this.edges = edges != null ? new ArrayList<>(edges) : new ArrayList<>();
    this.startNodeId = startNodeId;
    this.enabled = enabled != null ? enabled : false;  // 修改这里
}

// 在 getter 区域添加
public boolean isEnabled() { return enabled; }
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/powerflow/workflow/domain/model/Workflow.java
git commit -m "feat: add enabled field to Workflow"
```

---

## Task 6: 后端 - InMemoryWorkflowRepository 支持 enabled 查询

**Files:**
- Modify: `backend/src/main/java/com/powerflow/workflow/adapter/outbound/persistence/InMemoryWorkflowRepository.java`

- [ ] **Step 1: 添加 findByEnabled 方法**

```java
// 在现有方法后添加
public List<Workflow> findByEnabled(boolean enabled) {
    return workflows.values().stream()
        .filter(w -> w.isEnabled() == enabled)
        .collect(Collectors.toList());
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/powerflow/workflow/adapter/outbound/persistence/InMemoryWorkflowRepository.java
git commit -m "feat: add findByEnabled to InMemoryWorkflowRepository"
```

---

## Task 7: 后端 - TriggerSchedulerService 调度服务

**Files:**
- Create: `backend/src/main/java/com/powerflow/workflow/domain/service/TriggerSchedulerService.java`

- [ ] **Step 1: 创建 TriggerSchedulerService**

```java
package com.powerflow.workflow.domain.service;

import com.powerflow.workflow.domain.model.Workflow;
import com.powerflow.workflow.domain.model.enums.NodeType;
import com.powerflow.workflow.domain.model.enums.TriggerType;
import com.powerflow.workflow.domain.port.outbound.TriggerExecutionLogRepository;
import com.powerflow.workflow.domain.port.outbound.WorkflowRepository;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class TriggerSchedulerService {

    private final WorkflowRepository workflowRepository;
    private final TriggerExecutionLogRepository triggerLogRepository;
    private final WorkflowExecutor workflowExecutor;
    private final TaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public TriggerSchedulerService(WorkflowRepository workflowRepository,
                                   TriggerExecutionLogRepository triggerLogRepository,
                                   WorkflowExecutor workflowExecutor,
                                   TaskScheduler taskScheduler) {
        this.workflowRepository = workflowRepository;
        this.triggerLogRepository = triggerLogRepository;
        this.workflowExecutor = workflowExecutor;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void init() {
        refreshScheduledTasks();
    }

    public void refreshScheduledTasks() {
        // 取消所有现有任务
        scheduledTasks.values().forEach(f -> f.cancel(false));
        scheduledTasks.clear();

        // 重新注册所有启用定时的工作流
        workflowRepository.findByEnabled(true).forEach(workflow -> {
            Optional<Node> startNode = workflow.findNodeById(workflow.getStartNodeId());
            if (startNode.isPresent() && startNode.get().getType() == NodeType.START) {
                Map<String, Object> config = startNode.get().getConfig();
                String triggerType = (String) config.get("triggerType");
                if ("SCHEDULE".equals(triggerType)) {
                    String cron = (String) config.get("cron");
                    if (cron != null && !cron.isEmpty()) {
                        scheduleWorkflow(workflow.getId(), cron);
                    }
                }
            }
        });
    }

    public void scheduleWorkflow(String workflowId, String cronExpression) {
        ScheduledFuture<?> future = taskScheduler.schedule(
            () -> triggerScheduledWorkflow(workflowId),
            new CronTrigger(cronExpression)
        );
        scheduledTasks.put(workflowId, future);
    }

    public void unscheduleWorkflow(String workflowId) {
        ScheduledFuture<?> future = scheduledTasks.remove(workflowId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void triggerScheduledWorkflow(String workflowId) {
        workflowRepository.findById(workflowId).ifPresent(workflow -> {
            try {
                Context ctx = new Context(Map.of());
                WorkflowExecutionResult result = workflowExecutor.execute(workflow, ctx);
                triggerLogRepository.save(TriggerExecutionLog.builder()
                    .id(java.util.UUID.randomUUID().toString())
                    .workflowId(workflowId)
                    .triggerType(TriggerType.SCHEDULE)
                    .triggerSource((String) workflow.findNodeById(workflow.getStartNodeId())
                        .map(n -> (String) n.getConfig().get("cron"))
                        .orElse(""))
                    .status(result.getStatus())
                    .executionId(result.getExecutionId())
                    .triggeredAt(LocalDateTime.now())
                    .error(result.getError().orElse(null))
                    .build());
            } catch (Exception e) {
                triggerLogRepository.save(TriggerExecutionLog.builder()
                    .id(java.util.UUID.randomUUID().toString())
                    .workflowId(workflowId)
                    .triggerType(TriggerType.SCHEDULE)
                    .triggerSource((String) workflow.findNodeById(workflow.getStartNodeId())
                        .map(n -> (String) n.getConfig().get("cron"))
                        .orElse(""))
                    .status(com.powerflow.workflow.domain.model.enums.ExecutionStatus.FAILED)
                    .triggeredAt(LocalDateTime.now())
                    .error(e.getMessage())
                    .build());
            }
        });
    }
}
```

- [ ] **Step 2: 在 PowerflowApplication 添加 @EnableScheduling**

检查 `backend/src/main/java/com/powerflow/workflow/PowerflowApplication.java`，如果没有 `@EnableScheduling` 则添加：

```java
@SpringBootApplication
@EnableScheduling
public class PowerflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(PowerflowApplication.class, args);
    }
}
```

- [ ] **Step 3: 配置 TaskScheduler Bean**

创建 `backend/src/main/java/com/powerflow/workflow/config/SchedulerConfig.java`:

```java
package com.powerflow.workflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("trigger-scheduler-");
        return scheduler;
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/powerflow/workflow/domain/service/TriggerSchedulerService.java
git add backend/src/main/java/com/powerflow/workflow/config/SchedulerConfig.java
git add backend/src/main/java/com/powerflow/workflow/PowerflowApplication.java  # if modified
git commit -m "feat: add TriggerSchedulerService for cron-based workflow scheduling"
```

---

## Task 8: 后端 - WebhookController

**Files:**
- Create: `backend/src/main/java/com/powerflow/workflow/adapter/inbound/rest/WebhookController.java`

- [ ] **Step 1: 创建 WebhookController**

```java
package com.powerflow.workflow.adapter.inbound.rest;

import com.powerflow.workflow.domain.model.Context;
import com.powerflow.workflow.domain.model.Node;
import com.powerflow.workflow.domain.model.TriggerExecutionLog;
import com.powerflow.workflow.domain.model.Workflow;
import com.powerflow.workflow.domain.model.WorkflowExecutionResult;
import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import com.powerflow.workflow.domain.model.enums.NodeType;
import com.powerflow.workflow.domain.model.enums.TriggerType;
import com.powerflow.workflow.domain.port.outbound.TriggerExecutionLogRepository;
import com.powerflow.workflow.domain.port.outbound.WorkflowRepository;
import com.powerflow.workflow.domain.service.WorkflowExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final WorkflowRepository workflowRepository;
    private final TriggerExecutionLogRepository triggerLogRepository;
    private final WorkflowExecutor workflowExecutor;

    public WebhookController(WorkflowRepository workflowRepository,
                              TriggerExecutionLogRepository triggerLogRepository,
                              WorkflowExecutor workflowExecutor) {
        this.workflowRepository = workflowRepository;
        this.triggerLogRepository = triggerLogRepository;
        this.workflowExecutor = workflowExecutor;
    }

    @PostMapping("/{path}")
    public ResponseEntity<?> triggerWebhook(
            @PathVariable String path,
            @RequestBody Map<String, Object> payload,
            @RequestHeader Map<String, String> headers) {

        // 1. 查找匹配的工作流
        Workflow targetWorkflow = null;
        for (Workflow wf : workflowRepository.findAll()) {
            if (!wf.isEnabled()) continue;
            Node startNode = wf.findNodeById(wf.getStartNodeId()).orElse(null);
            if (startNode != null && startNode.getType() == NodeType.START) {
                String webhookPath = (String) startNode.getConfig().get("webhookPath");
                if (path.equals(webhookPath)) {
                    targetWorkflow = wf;
                    break;
                }
            }
        }

        if (targetWorkflow == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. 应用 fieldMappings 构建 Context
        Context ctx = buildContextFromPayload(targetWorkflow, payload);

        // 3. 执行工作流
        String executionId = UUID.randomUUID().toString();
        try {
            WorkflowExecutionResult result = workflowExecutor.execute(targetWorkflow, ctx);

            // 4. 记录 TriggerExecutionLog
            triggerLogRepository.save(TriggerExecutionLog.builder()
                .id(UUID.randomUUID().toString())
                .workflowId(targetWorkflow.getId())
                .triggerType(TriggerType.WEBHOOK)
                .triggerSource(path)
                .status(result.getStatus())
                .executionId(result.getExecutionId())
                .triggeredAt(LocalDateTime.now())
                .error(result.getError().orElse(null))
                .build());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "executionId", result.getExecutionId(),
                "status", result.getStatus()
            ));
        } catch (Exception e) {
            triggerLogRepository.save(TriggerExecutionLog.builder()
                .id(UUID.randomUUID().toString())
                .workflowId(targetWorkflow.getId())
                .triggerType(TriggerType.WEBHOOK)
                .triggerSource(path)
                .status(ExecutionStatus.FAILED)
                .triggeredAt(LocalDateTime.now())
                .error(e.getMessage())
                .build());

            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @SuppressWarnings("unchecked")
    private Context buildContextFromPayload(Workflow workflow, Map<String, Object> payload) {
        Node startNode = workflow.findNodeById(workflow.getStartNodeId()).orElse(null);
        if (startNode == null) {
            return new Context(Map.of());
        }

        Map<String, Object> fieldMappings = (Map<String, Object>) startNode.getConfig().get("fieldMappings");
        if (fieldMappings == null || fieldMappings.isEmpty()) {
            return new Context(Map.of("input", payload));
        }

        Map<String, Object> ctxData = new HashMap<>();
        for (Map.Entry<String, Object> entry : fieldMappings.entrySet()) {
            String sourceField = entry.getKey();
            String targetPath = (String) entry.getValue();
            Object value = payload.get(sourceField);
            setNestedValue(ctxData, targetPath, value);
        }

        return new Context(ctxData);
    }

    @SuppressWarnings("unchecked")
    private void setNestedValue(Map<String, Object> map, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = map;
        for (int i = 0; i < parts.length - 1; i++) {
            current.computeIfAbsent(parts[i], k -> new HashMap<>());
            current = (Map<String, Object>) current.get(parts[i]);
        }
        current.put(parts[parts.length - 1], value);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/powerflow/workflow/adapter/inbound/rest/WebhookController.java
git commit -m "feat: add WebhookController for webhook-triggered workflow execution"
```

---

## Task 9: 后端 - WorkflowController 新增 trigger-logs 端点

**Files:**
- Modify: `backend/src/main/java/com/powerflow/workflow/adapter/inbound/rest/WorkflowController.java`

- [ ] **Step 1: 添加 triggerLogs 方法**

在 WorkflowController 类中添加：

```java
@Autowired
private TriggerExecutionLogRepository triggerLogRepository;

@GetMapping("/trigger-logs")
public List<TriggerExecutionLog> getTriggerLogs(
        @RequestParam(required = false) String workflowId,
        @RequestParam(defaultValue = "50") int limit) {
    if (workflowId != null && !workflowId.isEmpty()) {
        return triggerLogRepository.findByWorkflowId(workflowId, limit);
    }
    return triggerLogRepository.findAll(limit);
}
```

注意：需要添加 import `java.util.List`。

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/powerflow/workflow/adapter/inbound/rest/WorkflowController.java
git commit -m "feat: add trigger-logs endpoint to WorkflowController"
```

---

## Task 10: 后端 - WorkflowExecutor 支持 Context 和 TriggerType

**Files:**
- Modify: `backend/src/main/java/com/powerflow/workflow/domain/service/WorkflowExecutor.java`

- [ ] **Step 1: 检查现有 execute 方法签名**

现有方法：
```java
public WorkflowExecutionResult execute(String workflowId, Context inputContext)
```

需要确保 Context 可以从 Map 创建。检查 `Context` 类是否有构造函数 `new Context(Map)`。

如果 Context 构造需要特殊处理，确保 webhook/scheduler 传入的 Context 能正常工作。

- [ ] **Step 2: 提交（无修改）**

如果现有代码已经支持，这一步跳过。

---

## Task 11: 后端 - WorkflowDataInitializer 添加 START 节点示例

**Files:**
- Modify: `backend/src/main/java/com/powerflow/workflow/adapter/outbound/persistence/WorkflowDataInitializer.java`

- [ ] **Step 1: 添加使用 START 节点的新工作流示例**

在现有工作流后添加一个使用 START 节点的定时工作流示例：

```java
// 在 initializeWorkflows() 方法末尾添加
createWorkflow(
    "schedule-notification",
    "定时通知",
    "每日定时发送通知",
    "start_node",
    List.of(
        createStartNode("start_node", "开始", "SCHEDULE", "0 9 * * *", null,
            Map.of()),
        createDataNode("send_notification", "发送通知", NodeType.DATA_PROCESSING,
            Map.of("outputKey", "result", "expression", "'通知已发送'")),
        createDataNode("end", "结束", NodeType.DATA_PROCESSING,
            Map.of("outputKey", "done", "expression", "'完成'"))
    ),
    List.of(
        createEdge("e1", "start_node", "send_notification"),
        createEdge("e2", "send_notification", "end")
    )
);

createWorkflow(
    "webhook-order",
    "Webhook下单",
    "接收订单Webhook",
    "start_node",
    List.of(
        createStartNode("start_node", "开始", "WEBHOOK", null, "/webhook/order",
            Map.of("orderId", "input.orderId", "amount", "input.amount")),
        createDataNode("process_order", "处理订单", NodeType.DATA_PROCESSING,
            Map.of("outputKey", "result", "expression", "'订单已处理'")),
        createDataNode("end", "结束", NodeType.DATA_PROCESSING,
            Map.of("outputKey", "done", "expression", "'完成'"))
    ),
    List.of(
        createEdge("e1", "start_node", "process_order"),
        createEdge("e2", "process_order", "end")
    )
);
```

添加 `createStartNode` 辅助方法：

```java
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
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/powerflow/workflow/adapter/outbound/persistence/WorkflowDataInitializer.java
git commit -m "feat: add START node examples to WorkflowDataInitializer"
```

---

## Task 12: 前端 - NodeTypes.ts 新增 START

**Files:**
- Modify: `frontend/src/types/NodeTypes.ts`

- [ ] **Step 1: 添加 START 类型**

```typescript
export type NodeType =
  | 'DATA_PROCESSING'
  | 'CONDITION'
  | 'HTTP_REQUEST'
  | 'LLM_CALL'
  | 'PARALLEL'
  | 'FOREACH'
  | 'BRANCH'
  | 'SUBWORKFLOW'
  | 'TRY_CATCH'
  | 'RETRY'
  | 'START';
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/types/NodeTypes.ts
git commit -m "feat: add START node type to frontend"
```

---

## Task 13: 前端 - START 节点配置面板

**Files:**
- Modify: `frontend/src/components/NodeConfigPanel.tsx`

- [ ] **Step 1: 添加触发器配置 UI**

在 NodeConfigPanel 中，为 START 节点类型添加触发器配置面板：

```tsx
// 在 switch(node.type) 中添加 case 'START'
case 'START':
  return (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium mb-1">触发类型</label>
        <select
          value={(node.config as any).triggerType || 'NONE'}
          onChange={(e) => updateNodeConfig(node.id, { triggerType: e.target.value })}
          className="w-full border rounded px-3 py-2"
        >
          <option value="NONE">无</option>
          <option value="SCHEDULE">定时任务</option>
          <option value="WEBHOOK">Webhook</option>
        </select>
      </div>

      {(node.config as any).triggerType === 'SCHEDULE' && (
        <div>
          <label className="block text-sm font-medium mb-1">Cron 表达式</label>
          <input
            type="text"
            value={(node.config as any).cron || ''}
            onChange={(e) => updateNodeConfig(node.id, { cron: e.target.value })}
            placeholder="0 9 * * *"
            className="w-full border rounded px-3 py-2"
          />
          <p className="text-xs text-gray-500 mt-1">格式: 分 时 日 月 周</p>
        </div>
      )}

      {(node.config as any).triggerType === 'WEBHOOK' && (
        <>
          <div>
            <label className="block text-sm font-medium mb-1">Webhook 路径</label>
            <input
              type="text"
              value={(node.config as any).webhookPath || ''}
              onChange={(e) => updateNodeConfig(node.id, { webhookPath: e.target.value })}
              placeholder="/webhook/order"
              className="w-full border rounded px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">字段映射</label>
            <FieldMappingsEditor
              mappings={(node.config as any).fieldMappings || {}}
              onChange={(mappings) => updateNodeConfig(node.id, { fieldMappings: mappings })}
            />
          </div>
        </>
      )}
    </div>
  );
```

添加 FieldMappingsEditor 组件（在同一个文件或拆分）：

```tsx
// FieldMappingsEditor 组件
function FieldMappingsEditor({
  mappings,
  onChange
}: {
  mappings: Record<string, string>;
  onChange: (mappings: Record<string, string>) => void;
}) {
  const addMapping = () => {
    onChange({ ...mappings, '': '' });
  };

  const updateMapping = (oldKey: string, newKey: string, value: string) => {
    const newMappings = { ...mappings };
    delete newMappings[oldKey];
    newMappings[newKey] = value;
    onChange(newMappings);
  };

  const removeMapping = (key: string) => {
    const newMappings = { ...mappings };
    delete newMappings[key];
    onChange(newMappings);
  };

  return (
    <div className="space-y-2">
      {Object.entries(mappings).map(([key, value], index) => (
        <div key={index} className="flex gap-2 items-center">
          <input
            type="text"
            value={key}
            onChange={(e) => updateMapping(key, e.target.value, value)}
            placeholder="payload字段"
            className="flex-1 border rounded px-2 py-1 text-sm"
          />
          <span>→</span>
          <input
            type="text"
            value={value}
            onChange={(e) => updateMapping(key, key, e.target.value)}
            placeholder="input.target"
            className="flex-1 border rounded px-2 py-1 text-sm"
          />
          <button
            onClick={() => removeMapping(key)}
            className="text-red-500 hover:text-red-700"
          >
            ×
          </button>
        </div>
      ))}
      <button
        onClick={addMapping}
        className="text-sm text-blue-500 hover:text-blue-700"
      >
        + 添加映射
      </button>
    </div>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/components/NodeConfigPanel.tsx
git commit -m "feat: add trigger config UI for START node"
```

---

## Task 14: 前端 - WorkflowEditor 添加启用开关

**Files:**
- Modify: `frontend/src/pages/WorkflowEditor.tsx`

- [ ] **Step 1: 添加启用开关**

在 WorkflowEditor 页面中，找到工作流配置区域，添加启用开关：

```tsx
// 在工作流配置表单中添加
<div className="flex items-center gap-4">
  <label className="flex items-center gap-2">
    <input
      type="checkbox"
      checked={workflow.enabled || false}
      onChange={(e) => updateWorkflow({ enabled: e.target.checked })}
      disabled={!canEnableTrigger(workflow)}
      className="w-4 h-4"
    />
    <span className="text-sm font-medium">启用定时/Webhook</span>
  </label>
  {!canEnableTrigger(workflow) && (
    <span className="text-xs text-gray-500">
      (需要 START 节点配置触发器)
    </span>
  )}
</div>
```

添加 canEnableTrigger 辅助函数：

```tsx
function canEnableTrigger(workflow: Workflow): boolean {
  if (!workflow.nodes || !workflow.startNodeId) return false;
  const startNode = workflow.nodes[workflow.startNodeId];
  if (!startNode || startNode.type !== 'START') return false;
  const config = startNode.config as any;
  return config?.triggerType && config.triggerType !== 'NONE';
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/pages/WorkflowEditor.tsx
git commit -m "feat: add enabled toggle for workflow triggers"
```

---

## Task 15: 前端 - workflowsApi.ts 更新类型

**Files:**
- Modify: `frontend/src/api/workflowsApi.ts`

- [ ] **Step 1: 更新 Workflow 类型**

```typescript
export interface Workflow {
  id: string;
  name: string;
  description: string;
  startNodeId: string;
  enabled?: boolean;
  nodes: Record<string, Node>;
  edges: Edge[];
}
```

- [ ] **Step 2: 添加 TriggerExecutionLog 类型**

```typescript
export interface TriggerExecutionLog {
  id: string;
  workflowId: string;
  triggerType: 'NONE' | 'SCHEDULE' | 'WEBHOOK';
  triggerSource: string;
  status: ExecutionStatus;
  executionId: string;
  triggeredAt: string;
  error?: string;
}
```

- [ ] **Step 3: 添加 getTriggerLogs API**

```typescript
export const getTriggerLogs = (workflowId?: string, limit = 50) => {
  const params = new URLSearchParams();
  if (workflowId) params.append('workflowId', workflowId);
  params.append('limit', limit.toString());
  return api.get(`/trigger-logs?${params}`);
};
```

- [ ] **Step 4: 提交**

```bash
git add frontend/src/api/workflowsApi.ts
git commit -m "feat: update frontend API types for trigger support"
```

---

## Task 16: 前端 - 触发历史查询页面

**Files:**
- Create: `frontend/src/pages/TriggerLogsPage.tsx`

- [ ] **Step 1: 创建 TriggerLogsPage**

```tsx
import { useState, useEffect } from 'react';
import { getTriggerLogs, TriggerExecutionLog } from '../api/workflowsApi';

export function TriggerLogsPage() {
  const [logs, setLogs] = useState<TriggerExecutionLog[]>([]);
  const [workflowId, setWorkflowId] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadLogs();
  }, []);

  const loadLogs = async () => {
    setLoading(true);
    try {
      const data = await getTriggerLogs(workflowId || undefined);
      setLogs(data);
    } catch (error) {
      console.error('Failed to load trigger logs:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    loadLogs();
  };

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">触发历史</h1>

      <form onSubmit={handleSearch} className="mb-6 flex gap-4">
        <input
          type="text"
          value={workflowId}
          onChange={(e) => setWorkflowId(e.target.value)}
          placeholder="工作流 ID (可选)"
          className="flex-1 border rounded px-3 py-2"
        />
        <button
          type="submit"
          className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
        >
          查询
        </button>
      </form>

      {loading ? (
        <div className="text-center py-8 text-gray-500">加载中...</div>
      ) : (
        <div className="space-y-4">
          {logs.map((log) => (
            <div
              key={log.id}
              className={`p-4 border rounded ${
                log.status === 'SUCCESS' ? 'border-green-200 bg-green-50' :
                log.status === 'FAILED' ? 'border-red-200 bg-red-50' :
                'border-gray-200'
              }`}
            >
              <div className="flex justify-between items-start">
                <div>
                  <span className="font-medium">{log.workflowId}</span>
                  <span className="ml-3 text-sm text-gray-500">
                    {log.triggerType === 'SCHEDULE' ? '定时' :
                     log.triggerType === 'WEBHOOK' ? 'Webhook' : '手动'}
                  </span>
                </div>
                <span className="text-sm text-gray-500">
                  {new Date(log.triggeredAt).toLocaleString()}
                </span>
              </div>
              <div className="mt-2 text-sm text-gray-600">
                触发源: {log.triggerSource}
              </div>
              {log.error && (
                <div className="mt-2 text-sm text-red-600">
                  错误: {log.error}
                </div>
              )}
            </div>
          ))}
          {logs.length === 0 && (
            <div className="text-center py-8 text-gray-500">暂无数据</div>
          )}
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/pages/TriggerLogsPage.tsx
git commit -m "feat: add trigger logs page"
```

---

## Task 17: 集成测试

**Files:**
- Create: `backend/src/test/java/com/powerflow/workflow/integration/TriggerExecutionTest.java`

- [ ] **Step 1: 编写触发器集成测试**

```java
package com.powerflow.workflow.integration;

import com.powerflow.workflow.adapter.outbound.persistence.InMemoryWorkflowRepository;
import com.powerflow.workflow.adapter.outbound.persistence.InMemoryTriggerExecutionLogRepository;
import com.powerflow.workflow.domain.model.Context;
import com.powerflow.workflow.domain.model.Node;
import com.powerflow.workflow.domain.model.Workflow;
import com.powerflow.workflow.domain.model.enums.NodeType;
import com.powerflow.workflow.domain.model.enums.TriggerType;
import com.powerflow.workflow.domain.service.WorkflowExecutor;
import com.powerflow.workflow.domain.service.TriggerSchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TriggerExecutionTest {

    private InMemoryWorkflowRepository workflowRepository;
    private InMemoryTriggerExecutionLogRepository triggerLogRepository;
    private WorkflowExecutor workflowExecutor;
    private TriggerSchedulerService triggerSchedulerService;

    @BeforeEach
    void setUp() {
        workflowRepository = new InMemoryWorkflowRepository();
        triggerLogRepository = new InMemoryTriggerExecutionLogRepository();

        TaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        ((ThreadPoolTaskScheduler) taskScheduler).setPoolSize(1);
        ((ThreadPoolTaskScheduler) taskScheduler).initialize();

        workflowExecutor = new WorkflowExecutor(
            workflowRepository,
            triggerLogRepository,
            new ContextManager(),
            null  // nodeExecutor - for testing use direct execution
        );

        triggerSchedulerService = new TriggerSchedulerService(
            workflowRepository,
            triggerLogRepository,
            workflowExecutor,
            taskScheduler
        );
    }

    @Test
    void testScheduleWorkflowExecution() {
        // 创建带 START 节点的定时工作流
        Workflow workflow = createScheduleWorkflow();
        workflowRepository.save(workflow);

        // 验证调度任务已注册
        triggerSchedulerService.refreshScheduledTasks();

        // 手动触发调度
        triggerSchedulerService.triggerScheduledWorkflow(workflow.getId());

        // 验证触发日志已记录
        var logs = triggerLogRepository.findByWorkflowId(workflow.getId(), 10);
        assertFalse(logs.isEmpty());
        assertEquals(TriggerType.SCHEDULE, logs.get(0).getTriggerType());
    }

    @Test
    void testWebhookWorkflowExecution() {
        // 创建带 START 节点的 Webhook 工作流
        Workflow workflow = createWebhookWorkflow();
        workflowRepository.save(workflow);

        // 模拟 webhook 调用
        Map<String, Object> payload = Map.of(
            "orderId", "ORD123",
            "amount", 100.0
        );

        // 直接调用工作流执行
        var result = workflowExecutor.execute(workflow, new Context(Map.of("input", payload)));
        assertTrue(result.isSuccess());
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
            .id("test-schedule-wf")
            .name("测试定时工作流")
            .description("定时工作流测试")
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
                "webhookPath", "/webhook/test",
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
            .id("test-webhook-wf")
            .name("测试Webhook工作流")
            .description("Webhook工作流测试")
            .startNodeId("start")
            .nodes(List.of(startNode, endNode))
            .edges(List.of())
            .enabled(true)
            .build();
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
cd backend && mvn test -Dtest=TriggerExecutionTest
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/test/java/com/powerflow/workflow/integration/TriggerExecutionTest.java
git commit -m "test: add trigger execution integration tests"
```

---

## 自检清单

完成实现后，检查以下各项：

1. **Spec 覆盖检查** - 确认每项设计需求都有对应实现
2. **类型一致性检查** - 后端 TriggerType.SCHEDULE/WEBHOOK 与前端 'SCHEDULE'/'WEBHOOK' 是否一致
3. **API 端点检查** - POST /webhook/{path}, GET /trigger-logs 是否可用
4. **边界情况检查** - cron 错误、webhook path 重复、缺少映射字段是否处理

---

## 实施顺序

1. Task 1-4: 基础模型（TriggerType, TriggerExecutionLog, Repository）
2. Task 5-6: NodeType + Workflow.enabled
3. Task 7: TriggerSchedulerService
4. Task 8: WebhookController
5. Task 9: WorkflowController trigger-logs
6. Task 10-11: 执行器和示例数据
7. Task 12-16: 前端实现
8. Task 17: 集成测试

---

**Plan complete.**