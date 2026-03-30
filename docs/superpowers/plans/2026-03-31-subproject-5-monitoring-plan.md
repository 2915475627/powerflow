# 子项目 5：执行监控台

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development

**Goal:** 创建执行监控台，展示工作流执行记录、节点耗时、状态等信息。

**Architecture:** 实时展示执行历史，提供执行详情和时间线视图。

**Tech Stack:** React 18, React Query, TailwindCSS

---

## 文件结构

```
frontend/src/
├── pages/
│   └── ExecutionHistoryPage.tsx
├── components/
│   ├── ExecutionTimeline.tsx
│   └── NodeExecutionCard.tsx
```

---

## Task 1: API 扩展

**Files:**
- Modify: `frontend/src/api/workflow.ts`

- [ ] **Step 1: 添加查询执行记录的 API**

在 workflow.ts 中添加：
```ts
export const executionApi = {
  listByWorkflow: async (workflowId: string): Promise<WorkflowExecutionResult[]> => {
    const response = await api.get(`/executions/workflow/${workflowId}`);
    return response.data;
  },

  get: async (executionId: string): Promise<WorkflowExecutionResult> => {
    const response = await api.get(`/executions/${executionId}`);
    return response.data;
  },
};
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/api/workflow.ts
git commit -m "feat(subproject-5): add execution history API"
```

---

## Task 2: ExecutionHistoryPage

**Files:**
- Create: `frontend/src/pages/ExecutionHistoryPage.tsx`

- [ ] **Step 1: 创建 ExecutionHistoryPage**

```tsx
import { useState } from 'react';
import { useWorkflows } from '../hooks/useWorkflow';
import { WorkflowExecutionTimeline } from '../components/WorkflowExecutionTimeline';

export function ExecutionHistoryPage() {
  const { data: workflows } = useWorkflows();
  const [selectedWorkflowId, setSelectedWorkflowId] = useState<string>('');

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">执行监控</h1>

      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          选择工作流
        </label>
        <select
          value={selectedWorkflowId}
          onChange={(e) => setSelectedWorkflowId(e.target.value)}
          className="w-full rounded-md border border-gray-300 px-3 py-2"
        >
          <option value="">请选择工作流</option>
          {workflows?.map((wf) => (
            <option key={wf.id} value={wf.id}>
              {wf.name}
            </option>
          ))}
        </select>
      </div>

      {selectedWorkflowId && (
        <WorkflowExecutionTimeline workflowId={selectedWorkflowId} />
      )}

      {!selectedWorkflowId && (
        <div className="text-center py-10 text-gray-500">
          请选择一个工作流查看执行历史
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/pages/ExecutionHistoryPage.tsx
git commit -m "feat(subproject-5): add ExecutionHistoryPage"
```

---

## Task 3: WorkflowExecutionTimeline

**Files:**
- Create: `frontend/src/components/WorkflowExecutionTimeline.tsx`

- [ ] **Step 1: 创建 WorkflowExecutionTimeline**

```tsx
import { useExecutionHistory } from '../hooks/useWorkflow';
import { NodeExecutionCard } from './NodeExecutionCard';
import type { WorkflowExecutionResult } from '../types/workflow';

interface Props {
  workflowId: string;
}

export function WorkflowExecutionTimeline({ workflowId }: Props) {
  const { data: executions, isLoading, error } = useExecutionHistory(workflowId);

  if (isLoading) return <div className="text-center py-10">加载中...</div>;
  if (error) return <div className="text-red-500 py-10">加载失败</div>;

  return (
    <div className="space-y-6">
      {executions?.map((execution) => (
        <ExecutionTimelineCard key={execution.executionId} execution={execution} />
      ))}

      {(!executions || executions.length === 0) && (
        <div className="text-center py-10 text-gray-500">暂无执行记录</div>
      )}
    </div>
  );
}

function ExecutionTimelineCard({ execution }: { execution: WorkflowExecutionResult }) {
  const totalDuration = execution.nodeExecutions.reduce((sum, ne) => sum + ne.durationMs, 0);

  return (
    <div className="bg-white rounded-lg shadow overflow-hidden">
      <div className="px-6 py-4 border-b flex justify-between items-center">
        <div>
          <span className="font-medium text-gray-900">执行ID: {execution.executionId}</span>
          <span
            className={`ml-4 px-2 py-1 rounded text-xs ${
              execution.status === 'SUCCESS'
                ? 'bg-green-100 text-green-800'
                : 'bg-red-100 text-red-800'
            }`}
          >
            {execution.status === 'SUCCESS' ? '成功' : '失败'}
          </span>
        </div>
        <div className="text-sm text-gray-500">
          总耗时: {totalDuration}ms | 节点数: {execution.nodeExecutions.length}
        </div>
      </div>

      {execution.error && (
        <div className="px-6 py-3 bg-red-50 text-red-700 text-sm">
          错误: {execution.error}
        </div>
      )}

      <div className="px-6 py-4">
        <h4 className="text-sm font-medium text-gray-700 mb-3">节点执行详情</h4>
        <div className="space-y-3">
          {execution.nodeExecutions.map((ne, index) => (
            <NodeExecutionCard key={ne.id} nodeExecution={ne} index={index + 1} />
          ))}
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/components/WorkflowExecutionTimeline.tsx
git commit -m "feat(subproject-5): add WorkflowExecutionTimeline component"
```

---

## Task 4: NodeExecutionCard

**Files:**
- Create: `frontend/src/components/NodeExecutionCard.tsx`

- [ ] **Step 1: 创建 NodeExecutionCard**

```tsx
import type { NodeExecution } from '../types/workflow';

interface Props {
  nodeExecution: NodeExecution;
  index: number;
}

export function NodeExecutionCard({ nodeExecution, index }: Props) {
  return (
    <div className="flex items-start gap-4 p-3 bg-gray-50 rounded-lg">
      <div className="flex-shrink-0 w-8 h-8 rounded-full bg-indigo-100 flex items-center justify-center text-sm font-medium text-indigo-700">
        {index}
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex justify-between items-start">
          <span className="font-medium text-gray-900">节点: {nodeExecution.nodeId}</span>
          <span className="text-sm text-gray-500">{nodeExecution.durationMs}ms</span>
        </div>
        <div className="mt-1 text-sm text-gray-500">
          状态:{' '}
          <span
            className={`${
              nodeExecution.status === 'SUCCESS' ? 'text-green-600' : 'text-red-600'
            }`}
          >
            {nodeExecution.status === 'SUCCESS' ? '成功' : '失败'}
          </span>
        </div>
        {nodeExecution.error && (
          <div className="mt-2 text-sm text-red-600">{nodeExecution.error}</div>
        )}
        <div className="mt-2 text-xs text-gray-400">
          开始: {new Date(nodeExecution.startTime).toLocaleString()}
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/components/NodeExecutionCard.tsx
git commit -m "feat(subproject-5): add NodeExecutionCard component"
```

---

## Task 5: Hook 扩展和路由

**Files:**
- Modify: `frontend/src/hooks/useWorkflow.ts`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/Layout.tsx`

- [ ] **Step 1: 添加 useExecutionHistory hook**

```ts
export function useExecutionHistory(workflowId: string) {
  return useQuery({
    queryKey: ['execution-history', workflowId],
    queryFn: () => executionApi.listByWorkflow(workflowId),
    enabled: !!workflowId,
  });
}
```

- [ ] **Step 2: 更新 App.tsx 添加监控路由**

```tsx
// 添加监控页面导入
import { ExecutionHistoryPage } from './pages/ExecutionHistoryPage';

// 添加路由
<Route path="history" element={<ExecutionHistoryPage />} />
```

- [ ] **Step 3: 更新 Layout 添加监控链接**

```tsx
<Link
  to="/history"
  className="px-3 py-2 rounded-md text-sm font-medium text-gray-700 hover:text-indigo-600"
>
  监控
</Link>
```

- [ ] **Step 4: 提交**

```bash
git add frontend/src/hooks/useWorkflow.ts frontend/src/App.tsx frontend/src/components/Layout.tsx
git commit -m "feat(subproject-5): add monitoring routes and hooks"
```

---

## Task 6: 最终验证并推送

- [ ] **Step 1: 运行构建验证**

Run: `cd frontend && npm run build`
Expected: BUILD SUCCESS

- [ ] **Step 2: 推送代码**

```bash
git add -A
git commit -m "feat(subproject-5): complete execution monitoring console"
git push origin subproject-5-monitoring
```

---

## 自检清单

- [ ] API 扩展执行记录查询
- [ ] ExecutionHistoryPage 执行历史页面
- [ ] WorkflowExecutionTimeline 时间线组件
- [ ] NodeExecutionCard 节点执行卡片
- [ ] useExecutionHistory hook
- [ ] 监控路由和导航
- [ ] 前端构建通过
- [ ] 代码推送到 GitHub
