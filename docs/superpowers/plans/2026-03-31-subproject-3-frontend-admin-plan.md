# 子项目 3：前端管理后台

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development

**Goal:** 创建 React + TypeScript 前端管理后台，实现工作流 CRUD、节点配置、节点测试界面。

**Architecture:** React 18 + TypeScript + Vite，前端与后端通过 REST API 通信。

**Tech Stack:** React 18, TypeScript, Vite, TailwindCSS, React Query, React Router

---

## 文件结构

```
frontend/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── index.html
├── src/
│   ├── main.tsx
│   ├── App.tsx
│   ├── api/
│   │   └── workflow.ts          # API 客户端
│   ├── components/
│   │   ├── Layout.tsx
│   │   ├── WorkflowList.tsx
│   │   ├── WorkflowEditor.tsx
│   │   ├── NodeConfig.tsx
│   │   └── NodeTester.tsx
│   ├── pages/
│   │   ├── WorkflowsPage.tsx
│   │   └── NodeTestPage.tsx
│   ├── types/
│   │   └── workflow.ts          # TypeScript 类型
│   └── hooks/
│       └── useWorkflow.ts       # React Query hooks
```

---

## Task 1: 前端项目初始化

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig.json`
- Create: `frontend/index.html`
- Create: `frontend/tailwind.config.js`
- Create: `frontend/src/main.tsx`
- Create: `frontend/src/vite-env.d.ts`

- [ ] **Step 1: 创建 package.json**

```json
{
  "name": "powerflow-frontend",
  "private": true,
  "version": "0.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.22.0",
    "@tanstack/react-query": "^5.17.0",
    "axios": "^1.6.0"
  },
  "devDependencies": {
    "@types/react": "^18.2.0",
    "@types/react-dom": "^18.2.0",
    "@vitejs/plugin-react": "^4.2.0",
    "autoprefixer": "^10.4.0",
    "postcss": "^8.4.0",
    "tailwindcss": "^3.4.0",
    "typescript": "^5.3.0",
    "vite": "^5.0.0"
  }
}
```

- [ ] **Step 2: 创建 vite.config.ts**

```ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

- [ ] **Step 3: 创建 tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

- [ ] **Step 4: 创建 tsconfig.node.json**

```json
{
  "compilerOptions": {
    "composite": true,
    "skipLibCheck": true,
    "module": "ESNext",
    "moduleResolution": "bundler",
    "allowSyntheticDefaultImports": true
  },
  "include": ["vite.config.ts"]
}
```

- [ ] **Step 5: 创建 index.html**

```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>PowerFlow - Workflow Management</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

- [ ] **Step 6: 创建 tailwind.config.js**

```js
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {},
  },
  plugins: [],
}
```

- [ ] **Step 7: 创建 postcss.config.js**

```js
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
}
```

- [ ] **Step 8: 创建 src/main.tsx**

```tsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import './index.css'

const queryClient = new QueryClient()

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>,
)
```

- [ ] **Step 9: 创建 src/index.css**

```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

- [ ] **Step 10: 创建 src/vite-env.d.ts**

```ts
/// <reference types="vite/client" />
```

- [ ] **Step 11: 安装依赖**

Run: `cd frontend && npm install`
Expected: 依赖安装成功

- [ ] **Step 12: 提交**

```bash
git add frontend/
git commit -m "feat(subproject-3): init React + Vite + TypeScript frontend"
```

---

## Task 2: 类型定义

**Files:**
- Create: `frontend/src/types/workflow.ts`

- [ ] **Step 1: 创建类型定义**

```ts
export type NodeType = 'DATA_PROCESSING' | 'CONDITION';
export type ExecutionStatus = 'SUCCESS' | 'FAILED';

export interface Context {
  data: Record<string, unknown>;
}

export interface Node {
  id: string;
  name: string;
  type: NodeType;
  config: Record<string, unknown>;
  inputMapping: Record<string, string>;
  outputMapping: Record<string, string>;
}

export interface Edge {
  id: string;
  fromNodeId: string;
  toNodeId: string;
  condition?: string;
}

export interface Workflow {
  id: string;
  name: string;
  description: string;
  nodes: Node[];
  edges: Edge[];
  startNodeId: string;
}

export interface NodeExecution {
  id: string;
  workflowExecutionId: string;
  nodeId: string;
  status: ExecutionStatus;
  input: Record<string, unknown>;
  output: Record<string, unknown>;
  error?: string;
  durationMs: number;
  startTime: string;
  endTime: string;
}

export interface WorkflowExecutionResult {
  workflowId: string;
  executionId: string;
  status: ExecutionStatus;
  finalContext: Context;
  nodeExecutions: NodeExecution[];
  error?: string;
}

export interface NodeResult {
  nodeId: string;
  status: ExecutionStatus;
  output: Record<string, unknown>;
  error?: string;
  nextNodeId?: string;
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/types/workflow.ts
git commit -m "feat(subproject-3): add TypeScript type definitions"
```

---

## Task 3: API 客户端

**Files:**
- Create: `frontend/src/api/workflow.ts`

- [ ] **Step 1: 创建 API 客户端**

```ts
import axios from 'axios';
import type { Workflow, Context, WorkflowExecutionResult, NodeResult } from '../types/workflow';

const api = axios.create({
  baseURL: '/api',
});

export const workflowApi = {
  list: async (): Promise<Workflow[]> => {
    const response = await api.get('/workflows');
    return response.data;
  },

  get: async (id: string): Promise<Workflow> => {
    const response = await api.get(`/workflows/${id}`);
    return response.data;
  },

  create: async (workflow: Workflow): Promise<Workflow> => {
    const response = await api.post('/workflows', workflow);
    return response.data;
  },

  update: async (workflow: Workflow): Promise<Workflow> => {
    const response = await api.put(`/workflows/${workflow.id}`, workflow);
    return response.data;
  },

  delete: async (id: string): Promise<void> => {
    await api.delete(`/workflows/${id}`);
  },

  execute: async (workflowId: string, context: Context): Promise<WorkflowExecutionResult> => {
    const response = await api.post(`/workflows/${workflowId}/execute`, context);
    return response.data;
  },

  testNode: async (workflowId: string, nodeId: string, context: Context): Promise<NodeResult> => {
    const response = await api.post(`/workflows/${workflowId}/nodes/${nodeId}/test`, context);
    return response.data;
  },
};
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/api/workflow.ts
git commit -m "feat(subproject-3): add workflow API client"
```

---

## Task 4: React Query Hooks

**Files:**
- Create: `frontend/src/hooks/useWorkflow.ts`

- [ ] **Step 1: 创建 React Query hooks**

```ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { workflowApi } from '../api/workflow';
import type { Workflow, Context } from '../types/workflow';

export function useWorkflows() {
  return useQuery({
    queryKey: ['workflows'],
    queryFn: workflowApi.list,
  });
}

export function useWorkflow(id: string) {
  return useQuery({
    queryKey: ['workflows', id],
    queryFn: () => workflowApi.get(id),
    enabled: !!id,
  });
}

export function useCreateWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (workflow: Workflow) => workflowApi.create(workflow),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['workflows'] });
    },
  });
}

export function useDeleteWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => workflowApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['workflows'] });
    },
  });
}

export function useExecuteWorkflow() {
  return useMutation({
    mutationFn: ({ workflowId, context }: { workflowId: string; context: Context }) =>
      workflowApi.execute(workflowId, context),
  });
}

export function useTestNode() {
  return useMutation({
    mutationFn: ({ workflowId, nodeId, context }: { workflowId: string; nodeId: string; context: Context }) =>
      workflowApi.testNode(workflowId, nodeId, context),
  });
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/hooks/useWorkflow.ts
git commit -m "feat(subproject-3): add React Query hooks"
```

---

## Task 5: Layout 组件

**Files:**
- Create: `frontend/src/components/Layout.tsx`

- [ ] **Step 1: 创建 Layout 组件**

```tsx
import { Link, Outlet } from 'react-router-dom';

export function Layout() {
  return (
    <div className="min-h-screen bg-gray-100">
      <nav className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex">
              <Link to="/" className="flex items-center px-4 text-xl font-bold text-indigo-600">
                PowerFlow
              </Link>
              <div className="flex space-x-4 ml-10">
                <Link
                  to="/"
                  className="px-3 py-2 rounded-md text-sm font-medium text-gray-700 hover:text-indigo-600"
                >
                  工作流
                </Link>
              </div>
            </div>
          </div>
        </div>
      </nav>
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>
    </div>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/components/Layout.tsx
git commit -m "feat(subproject-3): add Layout component"
```

---

## Task 6: App 路由

**Files:**
- Create: `frontend/src/App.tsx`

- [ ] **Step 1: 创建 App.tsx**

```tsx
import { Routes, Route } from 'react-router-dom';
import { Layout } from './components/Layout';
import { WorkflowsPage } from './pages/WorkflowsPage';
import { NodeTestPage } from './pages/NodeTestPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<WorkflowsPage />} />
        <Route path="test/:workflowId/:nodeId" element={<NodeTestPage />} />
      </Route>
    </Routes>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/App.tsx
git commit -m "feat(subproject-3): add App router"
```

---

## Task 7: WorkflowsPage - 工作流列表

**Files:**
- Create: `frontend/src/pages/WorkflowsPage.tsx`

- [ ] **Step 1: 创建 WorkflowsPage**

```tsx
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useWorkflows, useDeleteWorkflow } from '../hooks/useWorkflow';
import type { Workflow } from '../types/workflow';

export function WorkflowsPage() {
  const { data: workflows, isLoading, error } = useWorkflows();
  const deleteWorkflow = useDeleteWorkflow();
  const [showForm, setShowForm] = useState(false);
  const [newWorkflow, setNewWorkflow] = useState<Partial<Workflow>>({
    name: '',
    description: '',
    nodes: [],
    edges: [],
    startNodeId: '',
  });

  const handleCreate = () => {
    // TODO: 实现创建逻辑
    setShowForm(false);
  };

  const handleDelete = (id: string) => {
    if (confirm('确定要删除这个工作流吗？')) {
      deleteWorkflow.mutate(id);
    }
  };

  if (isLoading) return <div className="text-center py-10">加载中...</div>;
  if (error) return <div className="text-red-500 py-10">加载失败: {String(error)}</div>;

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-900">工作流管理</h1>
        <button
          onClick={() => setShowForm(true)}
          className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
        >
          创建工作流
        </button>
      </div>

      {showForm && (
        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <h2 className="text-lg font-semibold mb-4">新建工作流</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700">名称</label>
              <input
                type="text"
                value={newWorkflow.name}
                onChange={(e) => setNewWorkflow({ ...newWorkflow, name: e.target.value })}
                className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">描述</label>
              <textarea
                value={newWorkflow.description}
                onChange={(e) => setNewWorkflow({ ...newWorkflow, description: e.target.value })}
                className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2"
              />
            </div>
            <div className="flex space-x-4">
              <button
                onClick={handleCreate}
                className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
              >
                创建
              </button>
              <button
                onClick={() => setShowForm(false)}
                className="px-4 py-2 bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300"
              >
                取消
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                名称
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                描述
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                节点数
              </th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                操作
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {workflows?.map((workflow) => (
              <tr key={workflow.id}>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="text-sm font-medium text-gray-900">{workflow.name}</div>
                </td>
                <td className="px-6 py-4">
                  <div className="text-sm text-gray-500">{workflow.description}</div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {workflow.nodes.length}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <button
                    onClick={() => handleDelete(workflow.id)}
                    className="text-red-600 hover:text-red-900 mr-4"
                  >
                    删除
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {(!workflows || workflows.length === 0) && (
          <div className="text-center py-10 text-gray-500">暂无工作流</div>
        )}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/pages/WorkflowsPage.tsx
git commit -m "feat(subproject-3): add WorkflowsPage"
```

---

## Task 8: NodeTestPage - 节点测试页面

**Files:**
- Create: `frontend/src/pages/NodeTestPage.tsx`

- [ ] **Step 1: 创建 NodeTestPage**

```tsx
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useWorkflow, useTestNode } from '../hooks/useWorkflow';
import type { Context, NodeResult } from '../types/workflow';

export function NodeTestPage() {
  const { workflowId, nodeId } = useParams<{ workflowId: string; nodeId: string }>();
  const { data: workflow, isLoading } = useWorkflow(workflowId || '');
  const testNode = useTestNode();
  const [inputJson, setInputJson] = useState('{"amount": 1000}');
  const [result, setResult] = useState<NodeResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const node = workflow?.nodes.find((n) => n.id === nodeId);

  const handleTest = async () => {
    if (!workflowId || !nodeId) return;
    try {
      setError(null);
      const context: Context = { data: JSON.parse(inputJson) };
      const res = await testNode.mutateAsync({ workflowId, nodeId, context });
      setResult(res);
    } catch (err) {
      setError(err instanceof Error ? err.message : '测试失败');
    }
  };

  if (isLoading) return <div className="text-center py-10">加载中...</div>;
  if (!workflow || !node) return <div className="text-center py-10">工作流或节点不存在</div>;

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">测试节点</h1>
        <p className="text-gray-600">
          工作流: {workflow.name} / 节点: {node.name} ({node.type})
        </p>
      </div>

      <div className="grid grid-cols-2 gap-6">
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-semibold mb-4">节点配置</h2>
          <pre className="bg-gray-50 p-4 rounded-md overflow-auto">
            {JSON.stringify(node.config, null, 2)}
          </pre>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-semibold mb-4">输入映射</h2>
          <pre className="bg-gray-50 p-4 rounded-md overflow-auto">
            {JSON.stringify(node.inputMapping, null, 2)}
          </pre>
        </div>
      </div>

      <div className="mt-6 bg-white rounded-lg shadow p-6">
        <h2 className="text-lg font-semibold mb-4">执行测试</h2>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              输入上下文 (JSON)
            </label>
            <textarea
              value={inputJson}
              onChange={(e) => setInputJson(e.target.value)}
              className="w-full h-32 font-mono text-sm rounded-md border border-gray-300 px-3 py-2"
              placeholder='{"key": "value"}'
            />
          </div>
          <button
            onClick={handleTest}
            disabled={testNode.isPending}
            className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 disabled:bg-indigo-300"
          >
            {testNode.isPending ? '执行中...' : '执行节点'}
          </button>
        </div>

        {error && <div className="mt-4 p-4 bg-red-50 text-red-700 rounded-md">{error}</div>}

        {result && (
          <div className="mt-4">
            <h3 className="text-sm font-medium text-gray-700 mb-2">执行结果</h3>
            <pre className="bg-gray-50 p-4 rounded-md overflow-auto">
              {JSON.stringify(result, null, 2)}
            </pre>
          </div>
        )}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/pages/NodeTestPage.tsx
git commit -m "feat(subproject-3): add NodeTestPage"
```

---

## Task 9: 最终验证并推送

- [ ] **Step 1: 运行构建验证**

Run: `cd frontend && npm run build`
Expected: BUILD SUCCESS

- [ ] **Step 2: 推送代码**

```bash
git add -A
git commit -m "feat(subproject-3): complete frontend admin with workflow CRUD and node testing"
git push origin subproject-3-frontend-admin
```

---

## 自检清单

- [ ] React + Vite + TypeScript 项目初始化
- [ ] TypeScript 类型定义
- [ ] API 客户端
- [ ] React Query hooks
- [ ] Layout 组件
- [ ] App 路由
- [ ] WorkflowsPage 工作流列表
- [ ] NodeTestPage 节点测试
- [ ] 前端构建通过
- [ ] 代码推送到 GitHub
