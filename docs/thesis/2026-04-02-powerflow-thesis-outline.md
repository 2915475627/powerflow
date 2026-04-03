# PowerFlow 工作流引擎设计与实现 论文提纲

---

## 第1章 绪论

### 1.1 研究背景与意义
- 工作流引擎的概念与发展历程
- 现有工作流引擎（Activiti、Flowable、Camunda）的技术特点
- 传统工作流引擎在**DAG任务编排**场景下的局限性
  - 声明式配置 vs 可视化编排
  - 串行执行 vs 并行分支支持
  - 规则表达式嵌入能力
- 研究目的：设计并实现一个**轻量级DAG工作流执行引擎**，支持可视化节点编排、并行分支与规则引擎

### 1.2 论文结构安排
- 第1章：绪论
- 第2章：相关技术介绍
- 第3章：系统分析
- 第4章：系统设计
- 第5章：系统实现
- 第6章：系统测试
- 结论、参考文献、致谢

---

## 第2章 相关技术介绍

### 2.1 Java EE 与 Spring Boot 框架
- Spring Boot 核心特性（自动配置、starter 依赖）
- Spring AOP 面向切面编程（节点执行日志切面）

### 2.2 领域驱动设计（DDD）与六边形架构
- DDD 核心概念：聚合根、实体、值对象、领域服务
- 六边形架构（端口与适配器）：入站端口、出站端口
- 依赖倒置原则在架构中的应用

### 2.3 React 与 TypeScript
- React 函数式组件与Hooks
- TypeScript 类型系统（泛型、接口）
- React Flow 用于 DAG 可视化编辑器

### 2.4 DAG 拓扑排序算法
- 有向无环图（DAG）的概念
- Kahn 算法（基于入度的拓扑排序）
- 环检测算法（DFS着色法）

### 2.5 SpEL 规则引擎
- Spring Expression Language 基本语法
- 条件表达式的求值机制
- 在工作流节点条件路由中的应用

### 2.6 PostgreSQL 与 Redis
- PostgreSQL JSONB 字段存储工作流节点图结构
- Redis 作为执行上下文缓存

---

## 第3章 系统分析

### 3.1 需求分析

#### 3.1.1 功能需求
- **工作流管理**：创建、编辑、删除、启用/禁用工作流
- **节点编排**：支持 START、END、DATA_PROCESSING、CONDITION、PARALLEL、JOIN 六种节点类型
- **DAG可视化编辑**：拖拽节点、连线配置、缩放画布
- **工作流执行**：基于拓扑排序的DAG执行引擎，支持失败停止策略
- **上下文传递**：节点间通过 inputMapping/outputMapping 传递数据
- **条件路由**：CONDITION 节点支持 SpEL 表达式动态选择下一跳
- **并行分支**：PARALLEL + JOIN 节点支持并行执行与同步汇合
- **执行日志**：AOP 切面记录每个节点执行的输入、输出、耗时、状态

#### 3.1.2 非功能需求
- **可扩展性**：新增节点类型不影响现有架构（策略模式）
- **可观测性**：执行过程完整日志记录
- **性能**：单次执行延迟 < 100ms（排除节点业务逻辑）
- **可靠性**：DAG 环检测，执行失败可追溯

#### 3.1.3 用例分析
- 管理员：创建工作流 → 配置节点 → 启用工作流
- 用户：触发工作流执行 → 查看执行结果/执行历史
- 开发者：测试单个节点（输入上下文 → 执行 → 查看输出）

### 3.2 可行性分析

#### 3.2.1 技术可行性
- Java + Spring Boot 生态成熟，稳定可靠
- DDD 六边形架构已有大量实践
- React Flow 提供了 DAG 编辑器基础组件
- SpEL 内置于 Spring Boot，无需引入外部规则引擎

#### 3.2.2 经济可行性
- 所有技术栈均为开源，无需 licensing 费用
- 开发周期估算：5个子项目分阶段迭代

#### 3.2.3 操作可行性
- Web UI 操作界面友好
- 节点测试功能便于调试

---

## 第4章 系统设计

### 4.1 总体架构设计

采用 **DDD 六边形架构（端口-适配器）**，分层如下：

```
┌─────────────────────────────────────────┐
│            前端 React + TS              │
│   WorkflowEditorPage / NodeConfigPage   │
└──────────────────┬──────────────────────┘
                   │ HTTP/REST
┌──────────────────▼──────────────────────┐
│          入站适配器（Inbound Adapter）    │
│          REST API（Spring MVC）          │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│          入站端口（Inbound Port）         │
│         WorkflowUseCase 接口             │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│          领域层（Domain Layer）          │
│  WorkflowExecutor / NodeExecutor          │
│  ContextManager / RuleEvaluator           │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│          出站端口（Outbound Port）         │
│  WorkflowRepository / ExecutionLogRepository│
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│         出站适配器（Outbound Adapter）    │
│   JPA（PostgreSQL）/ Redis / 日志适配器  │
└─────────────────────────────────────────┘
```

### 4.2 核心域模型设计

#### 4.2.1 Workflow（工作流聚合根）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 唯一标识（UUID） |
| name | String | 工作流名称 |
| description | String | 描述 |
| nodes | Map<String, Node> | 节点集合（id → Node） |
| edges | List<Edge> | 连线集合 |
| startNodeId | String | 起始节点ID |
| enabled | boolean | 是否启用 |

#### 4.2.2 Node（节点实体）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 唯一标识 |
| name | String | 节点名称 |
| type | NodeType | 节点类型 |
| config | Map<String, Object> | 节点配置 |
| inputMapping | Map<String, String> | 上下文 → 节点输入映射 |
| outputMapping | Map<String, String> | 节点输出 → 上下文映射 |

#### 4.2.3 NodeType（节点类型枚举）
| 类型 | 说明 |
|------|------|
| START | 入口节点，无输入，唯一 |
| END | 结束节点，无输出，唯一 |
| DATA_PROCESSING | 数据处理节点，执行 transform 表达式 |
| CONDITION | 条件分支节点，SpEL 表达式路由 |
| PARALLEL | 并行分支入口，fork 出多条执行路径 |
| JOIN | 并行汇合节点，等待所有并行分支完成 |

#### 4.2.4 Edge（连线值对象）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 唯一标识 |
| fromNodeId | String | 源节点ID |
| toNodeId | String | 目标节点ID |
| condition | String | 可选，SpEL 表达式（CONDITION 节点使用） |

#### 4.2.5 Context（执行上下文）
| 字段 | 类型 | 说明 |
|------|------|------|
| data | Map<String, Object> | 扁平 KV，支持嵌套（user.name） |

#### 4.2.6 NodeExecution（节点执行记录）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 唯一标识 |
| workflowExecutionId | String | 所属工作流执行ID |
| nodeId | String | 节点ID |
| status | ExecutionStatus | SUCCESS / FAILED |
| input | Map<String, Object> | 节点输入 |
| output | Map<String, Object> | 节点输出 |
| error | String | 错误信息 |
| durationMs | long | 执行耗时（毫秒） |
| startTime | LocalDateTime | 开始时间 |
| endTime | LocalDateTime | 结束时间 |

### 4.3 数据库设计

#### 4.3.1 workflows 表
```sql
CREATE TABLE workflows (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    nodes JSONB NOT NULL,
    edges JSONB NOT NULL,
    start_node_id VARCHAR(36),
    enabled BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 4.3.2 node_executions 表
```sql
CREATE TABLE node_executions (
    id VARCHAR(36) PRIMARY KEY,
    workflow_execution_id VARCHAR(36) NOT NULL,
    node_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    input_data JSONB,
    output_data JSONB,
    error TEXT,
    duration_ms BIGINT,
    start_time TIMESTAMP,
    end_time TIMESTAMP
);
```

### 4.4 节点配置设计

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

**JOIN 节点**：
```json
{
  "waitAll": true
}
```

### 4.5 执行引擎核心算法设计

#### 4.5.1 DAG 构建与环检测
```
1. 从 startNodeId 出发，DFS 遍历所有节点
2. 访问时标记节点；若已访问但未完成，则检测到环 → 抛出 WorkflowValidationException
3. 拓扑排序得到节点执行顺序列表
```

#### 4.5.2 执行流程
```
1. 接收执行请求，注入 initialContext
2. DAG 构建 + 环检测 → 获得拓扑序
3. 遍历拓扑序执行每个节点：
   a. ContextManager 按 inputMapping 从上下文取值，构建节点输入
   b. AOP 前置通知：记录开始时间
   c. NodeExecutor.execute(node, context)：
      - DATA_PROCESSING：SpEL 求值 transform 表达式 → 结果写入 outputKey
      - CONDITION：SpEL 求值各条件表达式 → 确定下一跳（跳过无关分支）
      - PARALLEL：并发 fork 所有并行分支 → 等待 JOIN 汇合
      - JOIN：等待所有并行分支完成
      - START/END：无实际操作
   d. AOP 后置通知：记录结束时间、耗时、输出
   e. ContextManager 按 outputMapping 将结果写回上下文
4. 若节点执行失败 → 抛出 WorkflowExecutionException，终止执行
5. 返回 WorkflowExecutionResult（含最终 Context + 所有 NodeExecution 记录）
```

---

## 第5章 系统实现

### 5.1 后端实现

#### 5.1.1 项目结构
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
│   │       ├── NodeType.java
│   │       └── ExecutionStatus.java
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
│   │       └── WorkflowController.java
│   └── outbound/
│       ├── persistence/
│       ├── cache/
│       └── logging/
└── aop/
    └── NodeExecutionAspect.java
```

#### 5.1.2 关键实现

**WorkflowExecutor（工作流执行器）**：
- 构建 DAG（邻接表表示）
- DFS 环检测
- 拓扑排序得到执行序列
- 按序遍历执行节点，失败即停止

**NodeExecutor（节点执行器）**：
- 策略模式分发到具体节点类型处理器
- DATA_PROCESSING：调用 RuleEvaluator 求值 transform 表达式
- CONDITION：遍历 conditions 求值 SpEL 表达式，找到匹配分支
- PARALLEL：使用 CompletableFuture 并发执行所有分支
- JOIN：等待所有并行分支完成（CountDownLatch）

**RuleEvaluator（规则求值器）**：
- 使用 SpELTemplate 求值表达式
- 变量绑定：input → 节点输入 Map

**ContextManager（上下文管理器）**：
- inputMapping：key 为 Context 中的 key，value 为节点输入参数名
- outputMapping：key 为节点输出字段，value 为 Context 中的 key
- 支持嵌套 key（user.name → context.get("user").get("name")）

**NodeExecutionAspect（AOP切面）**：
- @Around("execution(* NodeExecutor.execute(..))")
- 执行前记录 startTime
- 执行后记录 endTime、durationMs、output/error
- 通过 ExecutionLogRepository 持久化

#### 5.1.3 REST API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/workflows | 创建工作流 |
| GET | /api/workflows | 查询所有工作流 |
| GET | /api/workflows/{id} | 查询单个工作流 |
| PUT | /api/workflows/{id} | 更新工作流 |
| DELETE | /api/workflows/{id} | 删除工作流 |
| POST | /api/workflows/{id}/execute | 触发工作流执行 |
| POST | /api/workflows/{id}/nodes/{nodeId}/test | 测试单个节点 |
| GET | /api/executions/{workflowExecutionId} | 查询执行记录 |
| GET | /api/workflows/{id}/executions | 查询工作流执行历史 |

### 5.2 前端实现

#### 5.2.1 技术栈
- React 18 + TypeScript + Vite
- React Flow（DAG 可视化画布）
- React Query（服务端状态管理）
- Tailwind CSS（样式）

#### 5.2.2 页面结构

| 页面 | 功能 |
|------|------|
| WorkflowsPage | 工作流列表（CRUD） |
| WorkflowEditorPage | DAG 可视化编辑器（拖拽、连线、缩放） |
| NodeConfigPage | 节点配置面板（节点类型表单、输入输出映射） |
| NodeTestPage | 节点测试界面（输入JSON → 执行 → 输出JSON） |
| TriggerLogsPage | 执行日志与历史查询 |

#### 5.2.3 关键实现

**DAG 画布（WorkflowEditorPage）**：
- 使用 React Flow 的 `<Background />`、`<Controls />`、`<MiniMap />`
- 自定义节点组件：每种 NodeType 对应不同的可视化样式
- 边的自定义：CONDITION 边上显示条件表达式标签
- 工具栏：添加节点（START/END/DATA_PROCESSING/CONDITION/PARALLEL/JOIN）、删除、缩放适应

**节点配置面板（NodeConfigPanel）**：
- 根据选中节点的 NodeType 动态渲染配置表单
- DATA_PROCESSING：表达式输入框（transform）、输出 key 输入框
- CONDITION：条件列表编辑器（添加/删除条件行）
- 实时预览：修改配置后画布节点样式同步更新

**节点测试（NodeTestPage）**：
- 左侧：输入 Context（JSON 编辑器）
- 中间：执行按钮
- 右侧：输出结果（JSON）+ 执行状态、耗时

---

## 第6章 系统测试

### 6.1 测试环境
- 硬件：MacBook Pro M3 / 16GB RAM
- JDK 17 + Node.js 20
- PostgreSQL 15 / Redis 7

### 6.2 功能测试

#### 6.2.1 单元测试

| 测试对象 | 测试内容 | 预期结果 |
|---------|---------|---------|
| WorkflowExecutor | DAG 环检测 | 含环工作流抛出异常 |
| WorkflowExecutor | 正常拓扑序 | 节点按正确顺序执行 |
| NodeExecutor（DATA_PROCESSING） | transform 表达式求值 | 输入 × 0.9 = 输出 |
| NodeExecutor（CONDITION） | SpEL 条件路由 | 匹配表达式 → 对应下一跳 |
| ContextManager | inputMapping 取值 | 正确从上下文取值 |
| ContextManager | outputMapping 写回 | 结果正确写入上下文 |
| RuleEvaluator | SpEL 表达式求值 | 复杂表达式正确求值 |

#### 6.2.2 集成测试
- 完整工作流执行（内存 Repository，不依赖 DB）
- PARALLEL + JOIN 并行分支执行与汇合
- 执行失败时错误信息与部分执行记录正确保存

### 6.3 性能测试
- 测试不同节点数量的执行耗时（10/50/100/500 节点）
- 并行分支数对总执行时间的影响
- Redis 缓存对 Context 读取性能的提升

### 6.4 测试结果分析
- 给出性能曲线图
- 验证非功能需求是否满足

---

## 结论

### 7.1 研究总结
- 设计并实现了一个基于 DDD 六边形架构的 DAG 工作流执行引擎
- 支持 6 种节点类型（含并行分支 PARALLEL + JOIN）
- SpEL 规则引擎实现动态条件路由
- 完整的前后端实现：DAG 可视化编辑器 + 执行监控

### 7.2 创新点
1. **轻量级 DAG 执行引擎**：不依赖 BPMN，专注 DAG 任务编排
2. **六边形架构**：领域层与技术细节完全解耦，新增节点类型不影响架构
3. **并行分支同步机制**：PARALLEL + JOIN 配对实现真正的并行执行

### 7.3 不足与展望
- 目前仅支持同步执行，未来可扩展异步执行（消息队列）
- 节点类型较少，可扩展为插件式节点（HTTP调用、脚本执行等）
- 可增加工作流版本管理与回滚机制

---

## 参考文献

[1] 张辉 等. Spring Boot 实战. 人民邮电出版社, 2020.
[2] Vaughn Vernon. 领域驱动设计精粹. 电子工业出版社, 2018.
[3] React Flow Official Documentation. https://reactflow.dev/
[4] Spring Expression Language Guide. https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions
[5] PostgreSQL JSONB Documentation. https://www.postgresql.org/docs/current/datatype-json.html

---

## 致谢
