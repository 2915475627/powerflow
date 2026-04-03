# PowerFlow 论文内容对照参考

> 本文档将论文提纲与实际项目代码进行对照，帮助你在写作时快速找到对应的实现内容。

---

## 第2章 相关技术介绍

### 2.2 领域驱动设计（DDD）与六边形架构

**写作要点**：
- 六边形架构 = 端口-适配器模式（Ports & Adapters）
- 核心思想：领域（业务逻辑）在中心，外部依赖（数据库、API、UI）通过适配器连接
- 入站端口（Inbound Port）：外部系统调用领域的接口（如 WorkflowUseCase）
- 出站端口（Outbound Port）：领域需要外部能力时的接口（如 WorkflowRepository）

**项目对应**：
```
backend/src/main/java/com/powerflow/workflow/domain/port/
├── inbound/WorkflowUseCase.java      ← 入站端口接口
└── outbound/
    ├── WorkflowRepository.java        ← 出站端口接口
    ├── ExecutionLogRepository.java    ← 出站端口接口
    └── NodeExecutorPort.java          ← 出站端口接口
```

### 2.4 DAG 拓扑排序算法

**写作要点**：
- Kahn算法：每次选择入度为0的节点，删除其所有出边，重复直到所有节点处理完毕
- DFS环检测：节点状态 = 未访问 / 访问中（递归栈中）/ 已完成；访问中又遇到 → 环
- 拓扑排序结果 = 节点的执行顺序

**项目对应**：
```
backend/src/main/java/com/powerflow/workflow/domain/service/WorkflowExecutor.java
```
- `buildDAG()`：构建邻接表
- `hasCycle()`：DFS 环检测
- `topologicalSort()`：Kahn 算法

---

## 第4章 系统设计

### 4.2 核心域模型设计

#### 4.2.1 Workflow

**项目对应**：
```
backend/src/main/java/com/powerflow/workflow/domain/model/Workflow.java
```
- `id`：UUID
- `name`：工作流名称
- `description`：描述
- `nodes`：Map<String, Node>，节点集合
- `edges`：List<Edge>，连线集合
- `startNodeId`：起始节点ID
- `enabled`：是否启用

#### 4.2.2 Node

**项目对应**：
```
backend/src/main/java/com/powerflow/workflow/domain/model/Node.java
```
- `id`：节点唯一标识
- `name`：节点名称
- `type`：NodeType 枚举
- `config`：Map<String, Object>，节点配置
- `inputMapping`：Map<String, String>，上下文→节点输入映射
- `outputMapping`：Map<String, String>，节点输出→上下文映射

#### 4.2.3 NodeType 枚举

**项目对应**：
```
backend/src/main/java/com/powerflow/workflow/domain/model/enums/NodeType.java
```
```java
public enum NodeType {
    START,           // 起始节点
    END,             // 结束节点
    DATA_PROCESSING, // 数据处理节点
    CONDITION,       // 条件分支节点
    PARALLEL,       // 并行分支入口
    JOIN             // 并行汇合节点
}
```

### 4.3 数据库设计

**项目对应**：
```
backend/src/main/java/com/powerflow/workflow/adapter/outbound/persistence/
```
- `WorkflowEntity.java`：对应 workflows 表，nodes/edges 存为 JSONB
- `NodeExecutionEntity.java`：对应 node_executions 表

### 4.5 执行引擎核心算法设计

**项目对应**：
```
backend/src/main/java/com/powerflow/workflow/domain/service/
├── WorkflowExecutor.java    ← DAG构建、环检测、拓扑排序
├── NodeExecutor.java        ← 节点类型分发执行
├── ContextManager.java      ← 上下文读写映射
└── RuleEvaluator.java       ← SpEL表达式求值
```

---

## 第5章 系统实现

### 5.1.2 关键实现

#### WorkflowExecutor 实现细节

```java
// 关键方法：
public WorkflowExecutionResult execute(Workflow workflow, Context initialContext) {
    // 1. 构建DAG（邻接表）
    Map<String, List<String>> dag = buildDAG(workflow);

    // 2. 环检测
    if (hasCycle(dag)) {
        throw new WorkflowValidationException("Workflow contains cycle");
    }

    // 3. 拓扑排序
    List<String> executionOrder = topologicalSort(dag, workflow.getStartNodeId());

    // 4. 按序执行
    Context context = initialContext;
    for (String nodeId : executionOrder) {
        Node node = workflow.findNodeById(nodeId).orElseThrow();
        NodeResult result = nodeExecutor.execute(node, context);
        context = contextManager.applyOutputMapping(result, node.getOutputMapping());
        if (result.isFailed()) {
            throw new WorkflowExecutionException("Node " + nodeId + " failed");
        }
    }

    return WorkflowExecutionResult.of(context);
}
```

#### NodeExecutor 实现细节（策略模式）

```java
public NodeResult execute(Node node, Context context) {
    return switch (node.getType()) {
        case START, END -> executeStartOrEnd(node, context);
        case DATA_PROCESSING -> executeDataProcessing(node, context);
        case CONDITION -> executeCondition(node, context);
        case PARALLEL -> executeParallel(node, context);
        case JOIN -> executeJoin(node, context);
    };
}
```

#### RuleEvaluator 实现细节

```java
// 使用 SpEL 求值表达式
public Object evaluate(String expression, Map<String, Object> variables) {
    SpelExpressionParser parser = new SpelExpressionParser();
    Expression exp = parser.parseExpression(expression);
    EvaluationContext evalContext = new StandardEvaluationContext();
    variables.forEach(evalContext::setVariable);
    return exp.getValue(evalContext);
}
```

#### ContextManager 实现细节

```java
// inputMapping：从上下文取值
public Map<String, Object> resolveInput(Node node, Context context) {
    Map<String, Object> input = new HashMap<>();
    for (Map.Entry<String, String> entry : node.getInputMapping().entrySet()) {
        String contextKey = entry.getKey();      // 上下文中的key
        String paramName = entry.getValue();       // 节点输入参数名
        input.put(paramName, resolveNestedValue(context.getData(), contextKey));
    }
    return input;
}

// outputMapping：结果写回上下文
public Context applyOutputMapping(NodeResult result, Map<String, String> outputMapping) {
    Map<String, Object> newData = new HashMap<>(context.getData());
    for (Map.Entry<String, String> entry : outputMapping.entrySet()) {
        String outputField = entry.getKey();       // 节点输出字段
        String contextKey = entry.getValue();      // 上下文中的key
        newData.put(contextKey, result.getOutput().get(outputField));
    }
    return new Context(newData);
}
```

### 5.1.3 REST API 设计

**项目对应**：
```
backend/src/main/java/com/powerflow/workflow/adapter/inbound/rest/
├── WorkflowController.java
└── NodeController.java
```

**API 列表**：
| 方法 | 路径 | 功能 |
|------|------|------|
| POST | /api/workflows | 创建工作流 |
| GET | /api/workflows | 查询所有工作流 |
| GET | /api/workflows/{id} | 查询单个工作流 |
| PUT | /api/workflows/{id} | 更新工作流 |
| DELETE | /api/workflows/{id} | 删除工作流 |
| POST | /api/workflows/{id}/execute | 触发执行 |
| POST | /api/workflows/{id}/nodes/{nodeId}/test | 节点测试 |
| GET | /api/executions/{id} | 查询执行记录 |
| GET | /api/workflows/{id}/executions | 执行历史 |

### 5.2 前端实现

**项目结构**：
```
frontend/src/
├── pages/
│   ├── WorkflowsPage.tsx         ← 工作流列表
│   ├── WorkflowEditorPage.tsx    ← DAG编辑器
│   ├── NodeConfigPage.tsx        ← 节点配置
│   ├── NodeTestPage.tsx          ← 节点测试
│   └── TriggerLogsPage.tsx       ← 执行日志
├── components/
│   ├── Layout.tsx
│   ├── NodeConfigPanel.tsx        ← 节点配置面板
│   ├── nodes/                    ← 自定义节点组件
│   └── WorkflowExecutionTimeline.tsx
├── api/                          ← API调用
└── types/                        ← TypeScript类型定义
```

**关键技术**：
- React Flow：`useNodesState`, `useEdgesState`, `addEdge`, `Handle`
- React Query：`useQuery`, `useMutation` 管理服务端状态
- Tailwind CSS：样式

---

## 第6章 系统测试

### 6.2.1 单元测试

**项目对应**：
```
backend/src/test/java/com/powerflow/workflow/domain/model/
├── WorkflowTest.java         ← DAG环检测、拓扑排序测试
├── NodeTest.java             ← 节点配置测试
├── ContextTest.java          ← 上下文映射测试
├── EdgeTest.java             ← 连线测试
└── NodeExecutionTest.java    ← 节点执行记录测试
```

**测试示例**：
```java
@Test
void shouldDetectCycleInWorkflow() {
    // A → B → C → A (环)
    Workflow workflow = Workflow.builder()
        .nodes(List.of(nodeA, nodeB, nodeC))
        .edges(List.of(edgeAB, edgeBC, edgeCA))
        .startNodeId("A")
        .build();

    assertThatThrownBy(() -> executor.execute(workflow, new Context()))
        .isInstanceOf(WorkflowValidationException.class)
        .hasMessageContaining("cycle");
}
```

---

## 写作提示

### 核心创新点总结（方便在结论章节描述）

1. **DAG 执行引擎**：不依赖 BPMN 标准，轻量级实现环检测 + 拓扑排序
2. **六边形架构**：领域层与基础设施完全解耦，新增节点类型不影响架构
3. **并行分支同步**：PARALLEL + JOIN 配对，CountDownLatch 实现并行执行与汇合
4. **SpEL 规则引擎**：运行时动态求值条件表达式，支持复杂路由逻辑
5. **可视化编辑器**：React Flow 实现拖拽式 DAG 编辑，降低使用门槛

### 各章节代码引用方式

- **第4章（设计）**：展示关键类图、接口定义、配置结构（JSON）
- **第5章（实现）**：展示核心方法实现代码（伪代码或简化代码）
- **第6章（测试）**：展示测试用例代码和测试结果截图

### 避免的问题

1. **不要贴完整代码**：只贴关键片段，用 `...` 省略
2. **不要截图代码**：直接用等宽字体排版代码块
3. **图要自己画**：用 draw.io / PlantUML / Mermaid 画架构图，不要截图他人作品
4. **引用要规范**：所有技术概念第一次出现要注明来源
