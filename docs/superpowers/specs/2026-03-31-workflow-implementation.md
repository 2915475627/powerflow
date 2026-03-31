# PowerFlow 工作流系统实现文档

> **更新时间**: 2026-03-31
> **状态**: Subproject 1-5 核心功能已完成

---

## 一、系统架构

### 1.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        Frontend (React)                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐   │
│  │WorkflowsPage│  │WorkflowEditor│  │ExecutionHistory │   │
│  │  (列表/执行) │  │  (DAG画布)   │  │    (监控)       │   │
│  └─────────────┘  └─────────────┘  └─────────────────┘   │
└───────────────────────────┬─────────────────────────────────┘
                              │ REST API (/api)
┌─────────────────────────────▼─────────────────────────────────┐
│                     Backend (Spring Boot)                       │
│  ┌──────────────────────────────────────────────────────┐    │
│  │                   Hexagonal Architecture               │    │
│  │  ┌────────────┐    ┌────────────┐    ┌───────────┐  │    │
│  │  │   Domain   │◄───│   Ports   │◄───│  Adapters │  │    │
│  │  │   Layer    │    │  (Interfaces│   │  (REST)   │  │    │
│  │  └────────────┘    └────────────┘    └───────────┘  │    │
│  └──────────────────────────────────────────────────────┘    │
│  ┌──────────────────────────────────────────────────────┐    │
│  │  In-Memory Storage (暂用) │ PostgreSQL/Redis (后续)  │    │
│  └──────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端框架 | React + TypeScript | React 18, TS 5.3 |
| 前端状态 | TanStack Query | v5.17 |
| 前端画布 | @xyflow/react | v12.x |
| 后端框架 | Spring Boot | 3.2.3 |
| 后端语言 | Java | 17/23 |
| 构建工具 | Maven (后端) / Vite (前端) | - |
| 测试 | JUnit 5, AssertJ | - |

---

## 二、功能模块

### 2.1 后端模块 (Java)

#### 2.1.1 域模型 (Domain Model)

| 类名 | 路径 | 说明 |
|------|------|------|
| `Workflow` | `domain/model/` | 工作流，包含节点和边 |
| `Node` | `domain/model/` | 节点，支持 DATA_PROCESSING 和 CONDITION 类型 |
| `Edge` | `domain/model/` | 连接线，连接节点 |
| `Context` | `domain/model/` | 执行上下文，存储 KV 数据 |
| `NodeExecution` | `domain/model/` | 节点执行记录 |
| `NodeResult` | `domain/model/` | 节点执行结果 |
| `WorkflowExecutionResult` | `domain/model/` | 工作流执行结果 |
| `ExecutionStatus` | `domain/model/enums/` | 执行状态枚举 |
| `NodeType` | `domain/model/enums/` | 节点类型枚举 |

#### 2.1.2 领域服务 (Domain Services)

| 类名 | 路径 | 说明 |
|------|------|------|
| `WorkflowExecutor` | `domain/service/` | 工作流执行器，负责 DAG 拓扑执行 |
| `NodeExecutorService` | `domain/service/` | 节点执行器，处理 DATA_PROCESSING 和 CONDITION |
| `ContextManager` | `domain/service/` | 上下文管理器，处理输入输出映射 |
| `RuleEvaluator` | `domain/service/` | 规则求值器，使用 SpEL |

#### 2.1.3 端口接口 (Ports)

**入站端口 (Inbound)**:
- `WorkflowUseCase`: 定义 execute 和 testNode 接口

**出站端口 (Outbound)**:
- `WorkflowRepository`: 工作流持久化
- `ExecutionLogRepository`: 执行日志持久化
- `NodeExecutorPort`: 节点执行抽象

#### 2.1.4 适配器 (Adapters)

| 类名 | 路径 | 说明 |
|------|------|------|
| `WorkflowController` | `adapter/inbound/rest/` | REST API 控制器 |
| `ExecutionController` | `adapter/inbound/rest/` | 执行记录查询控制器 |
| `InMemoryWorkflowRepository` | `adapter/outbound/persistence/` | 内存工作流存储 |
| `InMemoryExecutionLogRepository` | `adapter/outbound/logging/` | 内存执行日志存储 |
| `NodeExecutionAspect` | `aop/` | AOP 切面，记录节点执行日志 |

#### 2.1.5 REST API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/workflows` | 列出所有工作流 |
| `POST` | `/api/workflows` | 创建工作流 |
| `GET` | `/api/workflows/{id}` | 获取单个工作流 |
| `DELETE` | `/api/workflows/{id}` | 删除工作流 |
| `POST` | `/api/workflows/{id}/execute` | 执行工作流 |
| `POST` | `/api/workflows/{id}/nodes/{nodeId}/test` | 测试单个节点 |
| `GET` | `/api/executions/workflow/{workflowId}` | 查询工作流的执行历史 |
| `GET` | `/api/executions/{executionId}` | 查询单个执行记录 |

### 2.2 前端模块 (React)

#### 2.2.1 页面组件 (Pages)

| 组件 | 路径 | 说明 |
|------|------|------|
| `WorkflowsPage` | `pages/` | 工作流列表，支持创建、删除、执行 |
| `WorkflowEditorPage` | `pages/` | DAG 可视化编辑器 |
| `NodeTestPage` | `pages/` | 节点测试页面 |
| `ExecutionHistoryPage` | `pages/` | 执行历史监控页面 |

#### 2.2.2 业务组件 (Components)

| 组件 | 路径 | 说明 |
|------|------|------|
| `Layout` | `components/` | 页面布局和导航 |
| `WorkflowExecutionTimeline` | `components/` | 执行时间线展示 |
| `NodeExecutionCard` | `components/` | 节点执行详情卡片 |

#### 2.2.3 Hooks

| Hook | 路径 | 说明 |
|------|------|------|
| `useWorkflows` | `hooks/` | 获取工作流列表 |
| `useWorkflow` | `hooks/` | 获取单个工作流 |
| `useCreateWorkflow` | `hooks/` | 创建工作流 |
| `useDeleteWorkflow` | `hooks/` | 删除工作流 |
| `useExecuteWorkflow` | `hooks/` | 执行工作流 |
| `useTestNode` | `hooks/` | 测试节点 |
| `useExecutionHistory` | `hooks/` | 查询执行历史 |

#### 2.2.4 API 服务

| 服务 | 路径 | 说明 |
|------|------|------|
| `workflowApi` | `api/` | 工作流 CRUD 和执行 API |
| `executionApi` | `api/` | 执行记录查询 API |

---

## 三、数据模型

### 3.1 节点配置示例

**DATA_PROCESSING 节点**:
```json
{
  "outputKey": "result",
  "expression": "#input.value * 2",
  "nextNodeId": "node-2"
}
```

**CONDITION 节点**:
```json
{
  "conditions": [
    {"expression": "#input.amount > 1000", "nextNodeId": "high-discount"},
    {"expression": "#input.amount > 500", "nextNodeId": "medium-discount"}
  ],
  "defaultNextNodeId": "low-discount"
}
```

### 3.2 Context 数据结构

```json
{
  "data": {
    "value": 100,
    "userId": "user-123"
  }
}
```

### 3.3 执行结果

```json
{
  "workflowId": "wf-1",
  "executionId": "uuid",
  "status": "SUCCESS",
  "finalContext": {"data": {"result": 200}},
  "nodeExecutions": [
    {
      "nodeId": "node-1",
      "status": "SUCCESS",
      "input": {},
      "output": {"result": 200},
      "durationMs": 5
    }
  ],
  "success": true
}
```

---

## 四、执行流程

### 4.1 工作流执行流程

```
1. 接收执行请求 (WorkflowController.execute)
2. 查找工作流 (WorkflowRepository.findById)
3. 按 startNodeId 开始遍历
4. 对每个节点:
   a. ContextManager.extractNodeInput - 提取输入
   b. NodeExecutorService.execute - 执行节点
   c. 记录 NodeExecution 到日志
   d. ContextManager.writeNodeOutput - 写入输出
   e. 根据 nextNodeId 获取下一节点
5. 保存执行结果 (WorkflowExecutionResult)
6. 返回执行结果
```

### 4.2 节点执行类型

**DATA_PROCESSING**:
- 使用 SpEL 表达式计算
- 支持算术运算、字符串处理等
- 通过 outputKey 输出结果

**CONDITION**:
- 依次匹配 conditions 中的表达式
- 返回首个满足条件的 nextNodeId
- 若都不满足，返回 defaultNextNodeId

---

## 五、项目结构

```
powerflow/
├── pom.xml                    # Maven 配置
├── src/main/java/
│   └── com/powerflow/workflow/
│       ├── PowerflowApplication.java
│       ├── adapter/
│       │   ├── inbound/rest/
│       │   │   ├── WorkflowController.java
│       │   │   └── ExecutionController.java
│       │   └── outbound/
│       │       ├── logging/
│       │       │   └── InMemoryExecutionLogRepository.java
│       │       └── persistence/
│       │           └── InMemoryWorkflowRepository.java
│       ├── aop/
│       │   └── NodeExecutionAspect.java
│       ├── domain/
│       │   ├── model/
│       │   │   ├── Context.java
│       │   │   ├── Edge.java
│       │   │   ├── Node.java
│       │   │   ├── NodeExecution.java
│       │   │   ├── NodeResult.java
│       │   │   ├── Workflow.java
│       │   │   ├── WorkflowExecutionResult.java
│       │   │   └── enums/
│       │   ├── port/
│       │   │   ├── inbound/
│       │   │   │   └── WorkflowUseCase.java
│       │   │   └── outbound/
│       │   │       ├── ExecutionLogRepository.java
│       │   │       ├── NodeExecutorPort.java
│       │   │       └── WorkflowRepository.java
│       │   └── service/
│       │       ├── ContextManager.java
│       │       ├── NodeExecutorService.java
│       │       ├── RuleEvaluator.java
│       │       └── WorkflowExecutor.java
│       └── exception/
│           ├── NodeExecutionException.java
│           ├── WorkflowExecutionException.java
│           └── WorkflowNotFoundException.java
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── api/
│       │   └── workflow.ts
│       ├── components/
│       │   ├── Layout.tsx
│       │   ├── NodeExecutionCard.tsx
│       │   └── WorkflowExecutionTimeline.tsx
│       ├── hooks/
│       │   └── useWorkflow.ts
│       ├── pages/
│       │   ├── ExecutionHistoryPage.tsx
│       │   ├── NodeTestPage.tsx
│       │   ├── WorkflowEditorPage.tsx
│       │   └── WorkflowsPage.tsx
│       ├── types/
│       │   └── workflow.ts
│       ├── App.tsx
│       └── main.tsx
└── docs/
    └── superpowers/
        ├── plans/
        └── specs/
```

---

## 六、后续扩展计划

### 6.1 待完成的功能

| 优先级 | 功能 | 说明 |
|--------|------|------|
| P0 | PostgreSQL 持久化 | 替换内存存储 |
| P0 | Redis 缓存 | Context 缓存加速 |
| P1 | 用户认证 | JWT/OAuth |
| P1 | 工作流版本管理 | 版本控制、回滚 |
| P2 | 节点类型扩展 | HTTP、数据库等 |
| P2 | 实时执行监控 | WebSocket/SSE |
| P3 | 分布式执行 | 多节点并行 |

### 6.2 技术债务

- [ ] 单元测试覆盖率提升至 80%+
- [ ] 集成测试完善
- [ ] API 文档 (OpenAPI/Swagger)
- [ ] 输入验证增强
- [ ] 错误处理统一化

### 6.3 架构优化

- [ ] 工作流编译时校验（环检测）
- [ ] 表达式引擎插件化
- [ ] 节点执行超时控制
- [ ] 执行重试机制

---

## 七、运行指南

### 7.1 启动后端

```bash
cd /Users/swufan/projects/github/powerflow
mvn spring-boot:run
# 访问 http://localhost:8080
```

### 7.2 启动前端

```bash
cd /Users/swufan/projects/github/powerflow/frontend
npm install
npm run dev
# 访问 http://localhost:5173
```

### 7.3 API 测试

```bash
# 创建工作流
curl -X POST http://localhost:8080/api/workflows \
  -H "Content-Type: application/json" \
  -d '{
    "id": "test-wf",
    "name": "Test Workflow",
    "startNodeId": "node1",
    "nodes": {
      "node1": {
        "id": "node1",
        "name": "Start",
        "type": "DATA_PROCESSING",
        "config": {"outputKey": "result", "expression": "#input.value * 2"}
      }
    },
    "edges": []
  }'

# 执行工作流
curl -X POST http://localhost:8080/api/workflows/test-wf/execute \
  -H "Content-Type: application/json" \
  -d '{"data": {"value": 100}}'
```

---

## 八、Git 分支说明

| 分支 | 说明 |
|------|------|
| `master` | 主分支 |
| `subproject-1-core-domain` | 核心域实现 |
| `subproject-2-api-persistence` | API 与持久化 |
| `subproject-3-frontend-admin` | 前端管理后台 |
| `subproject-4-visual-editor` | 可视化编辑器 |
| `subproject-5-monitoring` | 执行监控 (当前) |

---

**文档版本**: v1.0
**下次更新**: ，待扩展功能实现后更新
