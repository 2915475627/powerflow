# PowerFlow Maven Multi-Module 重构设计方案

## 1. 背景与目标

### 1.1 现状

PowerFlow 当前为单体 Spring Boot 项目（`backend/pom.xml`），所有代码位于 `com.powerflow.workflow` 包下，包括：
- 领域模型（domain/model）
- 领域服务（domain/service，含 WorkflowExecutor、NodeExecutor、ContextManager 等）
- 端口接口（domain/port/inbound、domain/port/outbound）
- 六边形适配器（adapter/inbound/rest、adapter/outbound/*）
- AOP 切面（aop/）
- 配置类（config/）
- 异常类（exception/）
- 处理器（domain/service/handler/，含 ParallelHandler、HttpRequestHandler、LlmCallHandler 等 11 个 Handler）

NodeType 已支持 14 种：`DATA_PROCESSING`, `CONDITION`, `HTTP_REQUEST`, `LLM_CALL`, `PARALLEL`, `FOREACH`, `BRANCH`, `SUBWORKFLOW`, `TRY_CATCH`, `RETRY`, `START`, `END`, `JOIN`

### 1.2 目标

- **Core 纯净化**：`powerflow-core` 零 Spring 依赖，纯 JDK 17 POJO，任何 Java 项目均可引入
- **模块可独立发布**：每个模块为独立 Maven artifact，可按需引入
- **SPI 插件机制**：节点实现通过 Java SPI（`ServiceLoader`）自动发现，无需手动注册
- **参照业界实践**：参考 n8n 的 `packages/` monorepo 结构和 Dify/Coze 的节点可扩展设计

---

## 2. 竞品架构参考

| 平台 | 核心语言 | 包结构 | 节点扩展方式 | 引擎可嵌入性 |
|------|---------|--------|------------|------------|
| n8n | Node.js | `packages/core`, `packages/workflow`, `packages/editor-ui` | 社区 npm 包 (`n8n-nodes-*`) | ❌ 不支持 |
| Dify | Python | 微服务（API + Worker） | Plugin 机制 | ❌ Python 微服务 |
| Coze | Golang | 微服务 + DDD | Plugin 机制 | ❌ Golang 微服务 |
| **PowerFlow** | **Java** | **`packages/` multi-module（本次设计）** | **SPI + Maven 依赖** | **✅ 纯 Java jar** |

PowerFlow 的差异化价值：**唯一可将工作流引擎作为 Java jar 嵌入外部项目的主流工作流方案**。

---

## 3. 模块结构设计

```
powerflow/                                  # 根 pom.xml（parent）
│
├── powerflow-core/                         # 纯净引擎，零 Spring 依赖
│   ├── src/main/java/
│   │   └── com/powerflow/core/
│   │       ├── domain/
│   │       │   ├── model/                 # Workflow, Node, Edge, Context, NodeExecution, NodeResult
│   │       │   ├── enums/                 # NodeType, ExecutionStatus
│   │       │   └── port/
│   │       │       ├── inbound/            # WorkflowEnginePort（入站端口）
│   │       │       └── outbound/           # WorkflowRepositoryPort, NodeExecutorPort, ExecutionLogPort（出站端口）
│   │       ├── engine/
│   │       │   ├── WorkflowEngine.java     # 核心执行器（构建DAG、拓扑排序、环检测）
│   │       │   ├── NodeDispatcher.java     # 节点分发器（根据NodeType分发到NodeExecutor）
│   │       │   ├── ContextManager.java     # 上下文读写与映射
│   │       │   └── spi/
│   │       │       └── NodeExecutorLoader.java  # SPI Loader实现
│   │       ├── validation/
│   │       │   ├── WorkflowValidator.java  # 校验接口
│   │       │   ├── CycleDetectionValidator.java  # 环检测
│   │       │   ├── StartNodeValidator.java      # 起始节点校验
│   │       │   └── ValidationChain.java   # 校验链（Composite模式）
│   │       └── exception/                  # 核心异常（不含Spring）
│   │           ├── WorkflowExecutionException.java
│   │           └── WorkflowValidationException.java
│   └── pom.xml
│
├── powerflow-nodes/                        # 标准节点实现，零 Spring 依赖
│   ├── src/main/java/
│   │   └── com/powerflow/nodes/
│   │       ├── DataProcessingNodeExecutor.java
│   │       ├── ConditionNodeExecutor.java
│   │       ├── ParallelNodeExecutor.java
│   │       ├── ForeachNodeExecutor.java
│   │       ├── BranchNodeExecutor.java
│   │       ├── SubworkflowNodeExecutor.java
│   │       ├── TryCatchNodeExecutor.java
│   │       ├── RetryNodeExecutor.java
│   │       ├── StartNodeExecutor.java
│   │       ├── EndNodeExecutor.java
│   │       ├── JoinNodeExecutor.java
│   │       └── HttpRequestNodeExecutor.java   # HTTP 调用（内置，轻量）
│   ├── src/main/resources/
│   │   └── META-INF/services/
│   │       └── com.powerflow.core.port.outbound.NodeExecutorPort
│   │           # 文件内容：列出所有 NodeExecutor 实现类
│   └── pom.xml
│
├── powerflow-ext-llm/                      # LLM 扩展节点（依赖 LangChain4j 等）
│   ├── src/main/java/
│   │   └── com/powerflow/ext/llm/
│   │       └── LlmCallNodeExecutor.java    # LLM 调用节点
│   ├── src/main/resources/META-INF/services/
│   └── pom.xml
│
├── powerflow-adapter-springboot/           # Spring Boot 自动装配 + REST API
│   ├── src/main/java/
│   │   └── com/powerflow/adapter/spring/
│   │       ├── autoconfiguration/          # Spring Boot 自动配置
│   │       │   └── PowerFlowAutoConfiguration.java
│   │       ├── inbound/
│   │       │   └── rest/                   # REST Controller（@RestController）
│   │       │       ├── WorkflowController.java
│   │       │       └── ExecutionController.java
│   │       ├── outbound/
│   │       │   ├── persistence/            # JPA Repository 实现
│   │       │   └── cache/                  # Redis 适配器
│   │       ├── aop/
│   │       │   └── NodeExecutionAspect.java # AOP 日志切面（Spring AOP）
│   │       └── adapter/                    # 六边形适配器实现
│   │           ├── WorkflowRepositoryAdapter.java
│   │           └── ExecutionLogAdapter.java
│   └── pom.xml
│
├── powerflow-example/                     # 示例应用
│   ├── src/main/java/
│   │   └── com/powerflow/example/
│   │       └── DemoApplication.java       # 演示如何只用 core jar 构建应用
│   └── pom.xml
│
└── pom.xml                                 # 父 pom.xml
```

---

## 4. 核心设计决策

### 4.1 powerflow-core 完全纯净

`powerflow-core` 不依赖 Spring、不依赖任何外部库，只有 JDK 17 标准库（`java.util`、`java.util.concurrent`、`java.io` 等）。这意味着：

- 任何 Java 项目（Maven/Gradle）都可以引入 `powerflow-core` jar
- 核心引擎可以在 Android、Kotlin Multiplatform、非 Spring 的 Quarkus/Micronauts 项目中使用
- 核心逻辑可通过单元测试直接验证，无需 Spring Test

### 4.2 SPI 节点注册机制

节点实现类通过 Java 标准 SPI（`ServiceLoader`）机制自动发现：

**`com.powerflow.core.port.outbound.NodeExecutorPort` 接口**：
```java
public interface NodeExecutorPort {
    NodeType supportedType();
    NodeResult execute(Node node, ExecutionContext context);
}
```

**节点实现类**（在 `powerflow-nodes` 的 `META-INF/services/` 下注册）：
```java
// 文件：META-INF/services/com.powerflow.core.port.outbound.NodeExecutorPort
// 内容：
com.powerflow.nodes.DataProcessingNodeExecutor
com.powerflow.nodes.ConditionNodeExecutor
com.powerflow.nodes.HttpRequestNodeExecutor
...
```

**运行时加载**（在 `powerflow-core` 的 `NodeExecutorLoader` 中）：
```java
ServiceLoader<NodeExecutorPort> loader = ServiceLoader.load(NodeExecutorPort.class);
for (NodeExecutorPort executor : loader) {
    dispatcher.register(executor.supportedType(), executor);
}
```

### 4.3 校验链内置于 core

环检测、起始节点唯一性等校验逻辑作为 `WorkflowValidator` 接口的组合实现，固化在 `powerflow-core` 中。这确保了任何引入 core 的项目都自动获得工作流合法性校验，无法绕过。

### 4.4 adapter-springboot 负责所有 Spring 特定逻辑

以下功能因依赖 Spring 而放在 `adapter-springboot` 中：
- REST Controller（`@RestController`）
- Spring AOP 切面（`@Aspect`、`@Around`）
- Spring Data JPA Repository
- Redis 缓存适配器
- Spring Bean 生命周期管理

---

## 5. 依赖关系

```
powerflow-example
    └── powerflow-adapter-springboot
            ├── powerflow-nodes
            └── powerflow-ext-llm

powerflow-adapter-springboot
    ├── powerflow-core  (provided scope，运行时由容器提供)
    ├── powerflow-nodes (编译期依赖)
    └── spring-boot-starter-*

powerflow-nodes
    └── powerflow-core  (provided)

powerflow-ext-llm
    ├── powerflow-core  (provided)
    └── langchain4j     (LLM SDK)

powerflow-core
    └── (仅 JDK 17，无外部依赖)
```

---

## 6. 迁移策略

### 6.1 步骤一：创建模块骨架

在根目录创建以下 Maven module 骨架，不移动任何代码：
- `powerflow-core`
- `powerflow-nodes`
- `powerflow-adapter-springboot`
- `powerflow-example`

### 6.2 步骤二：迁移 powerflow-core

将以下代码从 `backend/src/main/java/com/powerflow/workflow/` 迁移到 `powerflow-core/src/main/java/com/powerflow/core/`：
- `domain/model/`（Workflow、Node、Edge、Context、NodeExecution、NodeResult）
- `domain/enums/`（NodeType、ExecutionStatus）
- `domain/port/`（入站/出站端口接口）
- `domain/service/` 中的核心引擎类（WorkflowExecutor、ContextManager、RuleEvaluator）
- `domain/service/validation/`（校验链）
- `exception/`（核心异常）

**注意**：移除所有 `@Component`、`@Service`、`@Aspect`、`@Configuration` 等 Spring 注解。

### 6.3 步骤三：迁移 powerflow-nodes

将以下代码迁移到 `powerflow-nodes/src/main/java/com/powerflow/nodes/`：
- `domain/service/handler/` 下所有 Handler 实现（11个）
- 迁移时重命名，去掉 `@Component`，实现 `NodeExecutorPort` 接口

同时创建 `src/main/resources/META-INF/services/com.powerflow.core.port.outbound.NodeExecutorPort` 文件。

### 6.4 步骤四：创建 powerflow-adapter-springboot

创建 Spring Boot 自动配置模块：
- 引入 `powerflow-core`（provided scope）
- 创建 `WorkflowController`、`ExecutionController`
- 创建 `NodeExecutionAspect`（保留 Spring AOP 逻辑）
- 实现 `WorkflowRepositoryPort`、`ExecutionLogPort`

### 6.5 步骤五：创建 powerflow-example

创建示例模块，展示"不引入 Spring Boot adapter，只用 core jar"的使用方式：
```java
public class Demo {
    public static void main(String[] args) {
        WorkflowEngine engine = new WorkflowEngine();
        NodeExecutorLoader.loadNodesInto(engine);

        Workflow workflow = WorkflowLoader.fromJson(json);
        ExecutionContext ctx = engine.execute(workflow, initialContext);
    }
}
```

### 6.6 步骤六：删除旧 backend 模块

所有代码迁移完毕并验证通过后，删除 `backend/` 目录，用新模块替代。

---

## 7. 验证清单

迁移完成后，以下场景应全部通过：

- [ ] `powerflow-core` 在 IDEA 中无任何 Spring 依赖警告
- [ ] `powerflow-core` 的单元测试不启动 Spring 上下文
- [ ] `powerflow-example` 不引入 `powerflow-adapter-springboot`，但能正常执行工作流
- [ ] 新增一个 NodeExecutor，通过在 `META-INF/services` 注册即可被 engine 自动发现，无需修改 engine 代码
- [ ] 原有单元测试（WorkflowTest、ContextTest、NodeTest 等）全部通过
- [ ] `mvn clean install` 整个项目无报错

---

## 8. 关键文件变更对照

| 操作 | 文件 |
|------|------|
| 新建 | `pom.xml`（根 parent） |
| 新建 | `powerflow-core/pom.xml` |
| 新建 | `powerflow-nodes/pom.xml` |
| 新建 | `powerflow-nodes/src/main/resources/META-INF/services/com.powerflow.core.port.outbound.NodeExecutorPort` |
| 新建 | `powerflow-adapter-springboot/pom.xml` |
| 新建 | `powerflow-example/pom.xml` |
| 移动+重写 | `backend/src/.../domain/model/*.java` → `powerflow-core/.../domain/model/` |
| 移动+重写 | `backend/src/.../domain/service/*.java` → `powerflow-core/.../engine/` |
| 移动+重写 | `backend/src/.../domain/service/handler/*.java` → `powerflow-nodes/.../` |
| 删除 | `backend/` 整个目录 |
