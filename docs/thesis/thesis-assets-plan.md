# PowerFlow 论文图表与数据准备清单

> 以下是你在写论文前需要准备的所有图表和数据。推荐用 **draw.io**（免费）绘制所有图表，导出为 SVG 或 PNG 后插入论文。

---

## 需要你准备的图表（共 9 张）

### 图 1：系统总体架构图
**章节**：第4章 4.1 总体架构设计
**类型**：架构图（分层）
**内容**：六边形架构分层，从前端 → 入站适配器 → 入站端口 → 领域层 → 出站端口 → 出站适配器
**工具**：draw.io
**尺寸建议**：宽 800px 以上，保持比例
**备注入口**：参考 `docs/thesis/2026-04-02-powerflow-thesis-outline.md` 第4章的 ASCII 图

---

### 图 2：PowerFlow 功能模块图
**章节**：第3章 3.1.1 功能需求
**类型**：功能模块图
**内容**：展示系统模块划分
```
PowerFlow 工作流引擎
├── 工作流管理（创建/编辑/删除/启用）
├── 节点编排（START/END/DATA_PROCESSING/CONDITION/PARALLEL/JOIN）
├── DAG 可视化编辑（拖拽/连线/缩放）
├── 执行引擎（DAG 构建/拓扑排序/并行分支）
├── 上下文传递（inputMapping/outputMapping）
├── 规则引擎（SpEL 条件路由）
└── 执行监控（节点日志/耗时/历史）
```
**工具**：draw.io

---

### 图 3：数据库 ER 图
**章节**：第4章 4.3 数据库设计
**类型**：ER 图
**内容**：workflows 表和 node_executions 表的关系
- workflows 表字段（id, name, description, nodes JSONB, edges JSONB, start_node_id, enabled, created_at, updated_at）
- node_executions 表字段（id, workflow_execution_id, node_id, status, input_data JSONB, output_data JSONB, error, duration_ms, start_time, end_time）
- 关系：1 个 Workflow 对应 N 个 NodeExecution
**工具**：draw.io

---

### 图 4：工作流执行引擎流程图
**章节**：第4章 4.5 执行引擎核心算法设计
**类型**：流程图（Flowchart）
**内容**：展示执行流程的 5 个步骤
```
接收执行请求
    ↓
DAG 构建 + 环检测
    ↓（有环→抛异常）
拓扑排序
    ↓
遍历执行节点
  ├─ ContextManager 取输入
  ├─ AOP 记录开始时间
  ├─ NodeExecutor 执行
  ├─ AOP 记录结束时间
  └─ ContextManager 写回输出
    ↓（失败→终止）
返回执行结果
```
**工具**：draw.io

---

### 图 5：六边形架构包结构图
**章节**：第5章 5.1.1 后端项目结构
**类型**：包结构图（树形）
**内容**：展示 Java 包结构
```
com.powerflow.workflow
├── domain/
│   ├── model/ (Workflow, Node, Edge, Context, NodeExecution)
│   ├── service/ (WorkflowExecutor, NodeExecutor, ContextManager, RuleEvaluator)
│   └── port/
│       ├── inbound/ (WorkflowUseCase)
│       └── outbound/ (WorkflowRepository, ExecutionLogRepository)
├── adapter/
│   ├── inbound/rest/ (WorkflowController)
│   └── outbound/persistence/ (JPA实现)
└── aop/ (NodeExecutionAspect)
```
**工具**：draw.io 或直接文本描述

---

### 图 6：DAG 拓扑排序算法示意图
**章节**：第2章 2.4 DAG 拓扑排序算法
**类型**：算法示意图（手工绘制更清晰）
**内容**：
- 左侧：一个 DAG 示例（4-5 个节点，带箭头，标注入度）
- 右侧：Kahn 算法过程（每一步移除入度为 0 的节点）
- 标注：执行顺序
**工具**：draw.io，手工绘制更自然

---

### 图 7：React Flow DAG 编辑器截图
**章节**：第5章 5.2 前端实现
**类型**：截图（你项目的前端界面）
**内容**：WorkflowEditorPage 的运行截图
- 需要能看到节点、连线、工具栏
- 可以用浏览器开发者工具调整窗口大小后截图
**工具**：浏览器截图

---

### 图 8：节点测试界面截图
**章节**：第5章 5.2 前端实现
**类型**：截图
**内容**：NodeTestPage 的运行截图
- 左侧输入 Context JSON
- 中间执行按钮
- 右侧输出结果 JSON
**工具**：浏览器截图

---

### 图 9：执行引擎核心类图
**章节**：第5章 5.1.2 关键实现
**类型**：UML 类图
**内容**：展示关键类及其关系
```
WorkflowExecutor
├── -nodeExecutor: NodeExecutor
├── -contextManager: ContextManager
├── +execute(workflow, context): WorkflowExecutionResult
├── -buildDAG(workflow): Map<String, List<String>>
├── -hasCycle(dag): boolean
└── -topologicalSort(dag): List<String>

NodeExecutor
├── -ruleEvaluator: RuleEvaluator
├── +execute(node, context): NodeResult
├── +executeDataProcessing(node, context): NodeResult
├── +executeCondition(node, context): NodeResult
└── +executeParallel(node, context): NodeResult

RuleEvaluator
├── -parser: SpelExpressionParser
└── +evaluate(expression, variables): Object

ContextManager
├── +resolveInput(node, context): Map<String, Object>
└── +applyOutputMapping(result, outputMapping): Context
```
**工具**：draw.io（可用简单矩形框表示类）

---

## 需要你准备的数据表格（共 3 张）

### 表 1：功能需求列表
**章节**：第3章 3.1.1 功能需求
**格式**：
| 需求编号 | 需求名称 | 需求描述 | 优先级 |
|----------|----------|----------|--------|
| FR-001 | 工作流创建 | 支持创建新的工作流，配置名称和描述 | 高 |
| FR-002 | 节点配置 | 支持配置 START/END/DATA_PROCESSING/CONDITION/PARALLEL/JOIN 六种节点 | 高 |
| FR-003 | DAG 可视化编辑 | 支持拖拽节点、连接线、缩放画布 | 高 |
| ... | ... | ... | ... |
**你需要填充**：根据实际功能，补充完整的需求列表（建议 8-10 条）

---

### 表 2：单元测试用例表
**章节**：第6章 6.2.1 单元测试
**格式**：
| 测试编号 | 测试对象 | 测试内容 | 输入 | 预期输出 | 实际结果 |
|----------|----------|----------|------|----------|----------|
| UT-001 | WorkflowExecutor | DAG 环检测 | 含环工作流 | 抛出 WorkflowValidationException | 待补充 |
| UT-002 | WorkflowExecutor | 正常拓扑排序 | A→B→C 三节点 | 按 A→B→C 顺序执行 | 待补充 |
| UT-003 | NodeExecutor | DATA_PROCESSING 节点 | 输入 {amount:100}，transform: amount*0.9 | 输出 {result:90} | 待补充 |
| UT-004 | NodeExecutor | CONDITION 节点 | 输入 {amount:1500}，conditions: amount>1000→B | 下一跳为 B | 待补充 |
| UT-005 | ContextManager | inputMapping 取值 | mapping: {total:"amount"}, context: {amount:200} | {total:200} | 待补充 |
**你需要补充**：实际运行测试后填入"实际结果"列

---

### 表 3：性能测试数据表
**章节**：第6章 6.3 性能测试
**格式**：
| 节点数量 | 执行次数 | 平均耗时 | 最大耗时 | 最小耗时 |
|----------|----------|----------|----------|----------|
| 10 节点 | 100 次 | ? ms | ? ms | ? ms |
| 50 节点 | 100 次 | ? ms | ? ms | ? ms |
| 100 节点 | 100 次 | ? ms | ? ms | ? ms |
| 500 节点 | 10 次 | ? ms | ? ms | ? ms |
**并行分支测试**：
| 并行分支数 | 执行次数 | 总耗时 |
|------------|----------|---------|
| 2 分支 | 50 次 | ? ms |
| 4 分支 | 50 次 | ? ms |
| 8 分支 | 50 次 | ? ms |
**你需要做**：用 JMeter 或简单循环跑性能测试，填入实际数据

---

## 你需要运行的命令（生成测试数据）

```bash
# 1. 运行单元测试，获取测试用例结果
cd backend
./mvnw test 2>&1 | tee test-results.txt

# 2. 构建项目
./mvnw clean package -DskipTests

# 3. 性能测试（写一个简单循环测试脚本，触发多次执行）
# 在 PostgreSQL 中查询执行耗时：
SELECT
  node_id,
  AVG(duration_ms) as avg_duration,
  MAX(duration_ms) as max_duration,
  MIN(duration_ms) as min_duration,
  COUNT(*) as exec_count
FROM node_executions
GROUP BY node_id
ORDER BY exec_count DESC
LIMIT 20;
```

---

## 图表绘制工具推荐

| 工具 | 用途 | 费用 |
|------|------|------|
| **draw.io**（diagrams.net） | 所有图表，导出 SVG/PNG | 免费 |
| **PlantUML** | 架构图、类图、时序图，用文本生成 | 免费 |
| **Mermaid** | 在 Markdown 中直接画图 | 免费 |
| **ProcessOn** | 在线协作画图 | 免费版有限制 |

**推荐**：统一用 **draw.io**，所有图表风格保持一致（黑白或浅灰色调，避免彩色图表）。

---

## 图表插入论文时的格式要求

1. **命名规范**：图 3-1、表 2-3 等（章编号-顺序号）
2. **每个图表上方要有编号和标题**（居中），下方要有简短说明
3. **插入位置**：应在正文中首次引用该图表的位置之后
4. **引用格式**：正文写"如图 3-1 所示"、"见表 2-3"

---

## 图表清单汇总

| 编号 | 图/表 | 名称 | 状态 |
|------|-------|------|------|
| 图 1 | 图 | 系统总体架构图 | **待绘制** |
| 图 2 | 图 | PowerFlow 功能模块图 | **待绘制** |
| 图 3 | 图 | 数据库 ER 图 | **待绘制** |
| 图 4 | 图 | 工作流执行引擎流程图 | **待绘制** |
| 图 5 | 图 | 六边形架构包结构图 | **待绘制** |
| 图 6 | 图 | DAG 拓扑排序算法示意图 | **待绘制** |
| 图 7 | 图 | React Flow DAG 编辑器截图 | **待截图** |
| 图 8 | 图 | 节点测试界面截图 | **待截图** |
| 图 9 | 图 | 执行引擎核心类图 | **待绘制** |
| 表 1 | 表 | 功能需求列表 | **待补充** |
| 表 2 | 表 | 单元测试用例表 | **待运行测试** |
| 表 3 | 表 | 性能测试数据表 | **待运行测试** |

> 优先级：图 1 > 图 4 > 图 3 > 表 1 > 其他（可以在写完对应章节后再做）
