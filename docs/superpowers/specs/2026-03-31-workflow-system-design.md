# 工作流系统设计文档

## 概述

工作流系统采用 DDD 六边形架构，后端 Java Spring Boot，前端 React，基础设施 PostgreSQL + Redis。分 5 个子项目迭代构建。

## 子项目划分

| 顺序 | 子项目 | 范围 |
|------|--------|------|
| 1 | 核心域与执行引擎 | 节点、工作流、上下文、DAG 执行、规则引擎 |
| 2 | 后端 API 与持久化 | 六边形架构适配器、REST API、PG/Redis、AOP 日志 |
| 3 | 前端管理后台 | 工作流 CRUD、节点配置、节点测试界面 |
| 4 | 前端可视化编辑器 | DAG 画布、拖拽连线 |
| 5 | 执行监控台 | 实时状态、耗时展示、执行历史 |

---

## 子项目 1：核心域与执行引擎

### 1. 技术栈

- Java 17 + Maven
- Spring Boot（核心域作为 Spring Bean）
- JUnit 5 + AssertJ + Mockito（测试）
- SpEL（条件表达式求值）

### 2. 包结构

```
com.powerflow.workflow
├── domain/
│   ├── model/
│   │   ├── Workflow.java
│   │   ├── Node.java
│   │   ├── Edge.java
│   │   ├── Context.java
│   │   ├── NodeExecution.java
│   │   └── enums/
│   │       └── NodeType.java
│   ├── service/
│   │   ├── WorkflowExecutor.java
│   │   ├── NodeExecutor.java
│   │   ├── ContextManager.java
│   │   └── RuleEvaluator.java
│   └── port/
│       ├── inbound/
│       │   └── WorkflowUseCase.java
│       └── outbound/
│           ├── WorkflowRepository.java
│           ├── ExecutionLogRepository.java
│           └── NodeExecutorPort.java
├── adapter/
│   ├── inbound/
│   │   └── rest/
│   └── outbound/
│       ├── persistence/
│       ├── cache/
│       └── logging/
└── aop/
    └── NodeExecutionAspect.java
```

### 3. 域模型

#### Workflow（工作流）

```java
public class Workflow {
    private String id;
    private String name;
    private String description;
    private List<Node> nodes;
    private List<Edge> edges;
    private String startNodeId;
}
```

#### Node（节点）

```java
public class Node {
    private String id;
    private String name;
    private NodeType type;           // DATA_PROCESSING | CONDITION
    private Map<String, Object> config;  // 节点配置
    private Map<String, String> inputMapping;   // 上下文 key → 节点输入
    private Map<String, String> outputMapping;   // 节点输出 → 上下文 key
}
```

#### NodeType（枚举）

```java
public enum NodeType {
    DATA_PROCESSING,   // 数据处理节点
    CONDITION          // 条件分支节点
}
```

#### Edge（连线）

```java
public class Edge {
    private String id;
    private String fromNodeId;
    private String toNodeId;
    private String condition;  // 可选，SpEL 表达式
}
```

#### Context（上下文）

```java
public class Context {
    private Map<String, Object> data;  // 扁平 KV，支持命名空间扩展（user.name）
}
```

#### NodeExecution（节点执行记录）

```java
public class NodeExecution {
    private String id;
    private String workflowExecutionId;
    private String nodeId;
    private ExecutionStatus status;  // SUCCESS | FAILED
    private Map<String, Object> input;
    private Map<String, Object> output;
    private String error;
    private long durationMs;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
```

### 4. 节点配置示例

**DATA_PROCESSING 节点**：
```json
{
  "transform": "input.amount * 0.9",
  "outputKey": "discountedAmount"
}
```

**CONDITION 节点**：
```json
{
  "conditions": [
    { "expression": "input.amount > 1000", "nextNodeId": "node-2" },
    { "expression": "input.amount > 500", "nextNodeId": "node-3" }
  ],
  "defaultNextNodeId": "node-4"
}
```

### 5. 端口接口

#### 入站端口（用例）

```java
public interface WorkflowUseCase {
    WorkflowExecutionResult execute(String workflowId, Context inputContext);
    NodeResult testNode(String workflowId, String nodeId, Context inputContext);
}
```

#### 出站端口（基础设施）

```java
public interface WorkflowRepository {
    Optional<Workflow> findById(String id);
    Workflow save(Workflow workflow);
    void delete(String id);
}

public interface ExecutionLogRepository {
    void save(NodeExecution execution);
    Page<NodeExecution> findByWorkflowExecutionId(String id, PageRequest page);
}

public interface NodeExecutorPort {
    NodeResult execute(Node node, Context context);
}
```

### 6. 领域服务

#### WorkflowExecutor（工作流执行器）

职责：
- 构建 DAG（拓扑排序）
- 检测环
- 按拓扑序执行节点
- 失败立即停止

```java
@Service
public class WorkflowExecutor {
    public WorkflowExecutionResult execute(Workflow workflow, Context initialContext) {
        // 1. 构建 DAG
        // 2. 拓扑排序
        // 3. 遍历执行节点
        // 4. 失败抛出 WorkflowExecutionException
    }
}
```

#### NodeExecutor（节点执行器）

职责：
- 根据 NodeType 分派到对应处理器
- DATA_PROCESSING：执行 transform 表达式
- CONDITION：计算条件表达式，返回下一跳

```java
@Service
public class NodeExecutor {
    public NodeResult execute(Node node, Context context) {
        return switch (node.getType()) {
            case DATA_PROCESSING -> executeDataProcessing(node, context);
            case CONDITION -> executeCondition(node, context);
        };
    }
}
```

#### ContextManager（上下文管理器）

职责：
- 按 inputMapping 从上下文读取数据
- 按 outputMapping 将结果写入上下文
- 支持命名空间（user.name → Map.get("user").get("name")）

#### RuleEvaluator（规则求值器）

职责：
- 使用 SpEL 求值条件表达式
- 表达式作用域：input 变量绑定到节点输入

### 7. 执行流程

```
1. WorkflowService 接收执行请求
2. WorkflowExecutor 构建 DAG（检测环，不合法抛异常）
3. 按拓扑序遍历节点：
   a. ContextManager 获取节点输入（按 inputMapping）
   b. AOP 前置日志（NodeExecutionAspect 开始时间）
   c. NodeExecutor.execute() 执行节点
   d. AOP 后置日志（结束时间、耗时、输出）
   e. ContextManager 将输出写入 Context（按 outputMapping）
   f. 若是 CONDITION 节点，RuleEvaluator 计算下一跳
   g. 失败抛出 WorkflowExecutionException，终止执行
4. 返回 WorkflowExecutionResult（最终 Context + 执行记录）
```

### 8. AOP 设计

```java
@Aspect
@Component
public class NodeExecutionAspect {
    @Around("execution(* NodeExecutor.execute(..))")
    public Object aroundNodeExecution(ProceedingJoinPoint joinPoint) {
        // 1. 记录开始时间
        // 2. 执行节点
        // 3. 记录结束时间、耗时、结果/错误
        // 4. 保存 NodeExecution 到日志端口
    }
}
```

### 9. 测试策略

| 层次 | 测试内容 | 工具 |
|------|---------|------|
| 域单元测试 | WorkflowExecutor（DAG 构建、拓扑排序）、RuleEvaluator（表达式）、ContextManager | JUnit 5 + AssertJ + Mockito |
| 节点单元测试 | DATA_PROCESSING transform 计算、CONDITION 条件判断 | JUnit 5 + AssertJ |
| 集成测试 | 完整工作流执行（内存实现，不依赖 DB） | JUnit 5 + Spring Test |

---

## 子项目 2：后端 API 与持久化

- REST API（Spring MVC）
- JPA + PostgreSQL（WorkflowRepository、ExecutionLogRepository 实现）
- Redis（Context 缓存）
- 完整 AOP 日志持久化

## 子项目 3：前端管理后台

- React + TypeScript
- 工作流 CRUD
- 节点配置表单
- 节点测试界面（输入上下文、执行、查看输出）

## 子项目 4：前端可视化编辑器

- 画布式 DAG 编辑器（拖拽、连线）
- 节点类型可视化
- 缩放、拖拽画布

## 子项目 5：执行监控台

- 实时执行状态
- 节点耗时展示
- 执行历史查询

---

## 下一步

等待用户确认后，创建子项目 1 的实现计划。
