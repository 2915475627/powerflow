# 子项目 4：前端可视化编辑器

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development

**Goal:** 创建画布式 DAG 可视化编辑器，支持拖拽节点、连线、可视化工作流。

**Architecture:** 基于 React Flow 实现 DAG 编辑器，提供节点拖拽、边缘连接、画布缩放平移等功能。

**Tech Stack:** React 18, React Flow (@xyflow/react), TailwindCSS

---

## 文件结构

```
frontend/src/
├── components/
│   ├── WorkflowCanvas.tsx      # 主画布组件
│   ├── nodes/
│   │   ├── DataProcessingNode.tsx
│   │   └── ConditionNode.tsx
│   └── edges/
│       └── ConditionalEdge.tsx
├── pages/
│   └── WorkflowEditorPage.tsx
```

---

## Task 1: 安装 React Flow

**Files:**
- Modify: `frontend/package.json`

- [ ] **Step 1: 添加 @xyflow/react 依赖**

```bash
cd frontend && npm install @xyflow/react
```

- [ ] **Step 2: 提交**

```bash
git add frontend/package.json
git commit -m "feat(subproject-4): add @xyflow/react for DAG editor"
```

---

## Task 2: 自定义节点组件

**Files:**
- Create: `frontend/src/components/nodes/DataProcessingNode.tsx`
- Create: `frontend/src/components/nodes/ConditionNode.tsx`

- [ ] **Step 1: 创建 DataProcessingNode**

```tsx
import { memo } from 'react';
import { Handle, Position, NodeProps } from '@xyflow/react';

export const DataProcessingNode = memo(({ data, selected }: NodeProps) => {
  return (
    <div className={`px-4 py-3 rounded-lg border-2 min-w-[150px] ${
      selected ? 'border-indigo-500 bg-indigo-50' : 'border-gray-300 bg-white'
    }`}>
      <div className="flex items-center gap-2">
        <div className="w-3 h-3 rounded-full bg-green-500" />
        <span className="font-medium text-gray-900">{data.label || 'Data Processing'}</span>
      </div>
      <div className="mt-2 text-xs text-gray-500">
        {data.config?.expression?.toString().substring(0, 30) || 'No expression'}
      </div>
      <Handle
        type="target"
        position={Position.Top}
        className="w-4 h-4 border-2 border-gray-400 bg-white"
      />
      <Handle
        type="source"
        position={Position.Bottom}
        className="w-4 h-4 border-2 border-gray-400 bg-white"
      />
    </div>
  );
});

DataProcessingNode.displayName = 'DataProcessingNode';
```

- [ ] **Step 2: 创建 ConditionNode**

```tsx
import { memo } from 'react';
import { Handle, Position, NodeProps } from '@xyflow/react';

export const ConditionNode = memo(({ data, selected }: NodeProps) => {
  return (
    <div className={`px-4 py-3 rounded-lg border-2 min-w-[150px] ${
      selected ? 'border-indigo-500 bg-indigo-50' : 'border-orange-300 bg-orange-50'
    }`}>
      <div className="flex items-center gap-2">
        <div className="w-3 h-3 rounded-full bg-orange-500" />
        <span className="font-medium text-gray-900">{data.label || 'Condition'}</span>
      </div>
      <div className="mt-2 text-xs text-gray-500">
        {data.conditions?.length || 0} conditions
      </div>
      <Handle
        type="target"
        position={Position.Top}
        className="w-4 h-4 border-2 border-gray-400 bg-white"
      />
      <Handle
        type="source"
        position={Position.Left}
        className="w-4 h-4 border-2 border-gray-400 bg-white"
        id="left"
      />
      <Handle
        type="source"
        position={Position.Right}
        className="w-4 h-4 border-2 border-gray-400 bg-white"
        id="right"
      />
      <Handle
        type="source"
        position={Position.Bottom}
        className="w-4 h-4 border-2 border-gray-400 bg-white"
        id="default"
      />
    </div>
  );
});

ConditionNode.displayName = 'ConditionNode';
```

- [ ] **Step 3: 提交**

```bash
git add frontend/src/components/nodes/
git commit -m "feat(subproject-4): add custom node components"
```

---

## Task 3: WorkflowCanvas 组件

**Files:**
- Create: `frontend/src/components/WorkflowCanvas.tsx`

- [ ] **Step 1: 创建 WorkflowCanvas**

```tsx
import { useCallback, useState } from 'react';
import {
  ReactFlow,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  addEdge,
  Connection,
  Edge,
  Node,
  NodeTypes,
  OnNodesChange,
  OnEdgesChange,
  OnConnect,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { DataProcessingNode } from './nodes/DataProcessingNode';
import { ConditionNode } from './nodes/ConditionNode';

const nodeTypes: NodeTypes = {
  dataProcessing: DataProcessingNode,
  condition: ConditionNode,
};

export function WorkflowCanvas() {
  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);

  const onConnect: OnConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  const addDataProcessingNode = () => {
    const newNode: Node = {
      id: `node-${Date.now()}`,
      type: 'dataProcessing',
      position: { x: 250, y: 150 },
      data: {
        label: 'Data Processing',
        config: { expression: '#input.value * 1', outputKey: 'result' },
      },
    };
    setNodes((nds) => [...nds, newNode]);
  };

  const addConditionNode = () => {
    const newNode: Node = {
      id: `node-${Date.now()}`,
      type: 'condition',
      position: { x: 250, y: 150 },
      data: {
        label: 'Condition',
        conditions: [],
      },
    };
    setNodes((nds) => [...nds, newNode]);
  };

  return (
    <div className="h-[600px] border rounded-lg overflow-hidden">
      <div className="bg-gray-50 border-b p-4 flex gap-4">
        <button
          onClick={addDataProcessingNode}
          className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 text-sm"
        >
          + Data Processing
        </button>
        <button
          onClick={addConditionNode}
          className="px-4 py-2 bg-orange-600 text-white rounded-md hover:bg-orange-700 text-sm"
        >
          + Condition
        </button>
      </div>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        nodeTypes={nodeTypes}
        fitView
      >
        <Controls />
        <Background />
      </ReactFlow>
    </div>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/components/WorkflowCanvas.tsx
git commit -m "feat(subproject-4): add WorkflowCanvas with React Flow"
```

---

## Task 4: WorkflowEditorPage

**Files:**
- Create: `frontend/src/pages/WorkflowEditorPage.tsx`
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: 创建 WorkflowEditorPage**

```tsx
import { WorkflowCanvas } from '../components/WorkflowCanvas';

export function WorkflowEditorPage() {
  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-900">工作流编辑器</h1>
        <div className="flex gap-4">
          <button className="px-4 py-2 bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300">
            保存
          </button>
          <button className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700">
            执行
          </button>
        </div>
      </div>
      <WorkflowCanvas />
      <div className="mt-4 text-sm text-gray-500">
        <p>拖拽节点到画布，连接节点创建工作流流程。</p>
        <p className="mt-1">双击节点编辑属性。</p>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: 更新 App.tsx 添加路由**

```tsx
import { Routes, Route } from 'react-router-dom';
import { Layout } from './components/Layout';
import { WorkflowsPage } from './pages/WorkflowsPage';
import { NodeTestPage } from './pages/NodeTestPage';
import { WorkflowEditorPage } from './pages/WorkflowEditorPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<WorkflowsPage />} />
        <Route path="editor" element={<WorkflowEditorPage />} />
        <Route path="test/:workflowId/:nodeId" element={<NodeTestPage />} />
      </Route>
    </Routes>
  );
}
```

- [ ] **Step 3: 更新 Layout 添加编辑器链接**

在 Layout.tsx 中添加编辑器链接：
```tsx
<Link
  to="/editor"
  className="px-3 py-2 rounded-md text-sm font-medium text-gray-700 hover:text-indigo-600"
>
  编辑器
</Link>
```

- [ ] **Step 4: 提交**

```bash
git add frontend/src/pages/WorkflowEditorPage.tsx frontend/src/App.tsx frontend/src/components/Layout.tsx
git commit -m "feat(subproject-4): add WorkflowEditorPage with canvas"
```

---

## Task 5: 最终验证并推送

- [ ] **Step 1: 运行构建验证**

Run: `cd frontend && npm run build`
Expected: BUILD SUCCESS

- [ ] **Step 2: 推送代码**

```bash
git add -A
git commit -m "feat(subproject-4): complete visual DAG editor with React Flow"
git push origin subproject-4-visual-editor
```

---

## 自检清单

- [ ] @xyflow/react 依赖安装
- [ ] 自定义节点组件 DataProcessingNode
- [ ] 自定义节点组件 ConditionNode
- [ ] WorkflowCanvas 主画布
- [ ] WorkflowEditorPage 编辑器页面
- [ ] 路由配置
- [ ] 前端构建通过
- [ ] 代码推送到 GitHub
