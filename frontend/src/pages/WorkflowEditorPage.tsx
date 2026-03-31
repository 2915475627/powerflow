import { useCallback, useState, useMemo, type ChangeEvent } from 'react';
import {
  ReactFlow,
  MiniMap,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  addEdge,
  Connection,
  Edge,
  Node,
  BackgroundVariant,
  NodeTypes,
  Handle,
  Position,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { useCreateWorkflow, useWorkflow, useWorkflows, useNodeTemplates, useNodeTemplatesByType } from '../hooks/useWorkflow';
import { useNavigate, useSearchParams } from 'react-router-dom';
import type { Workflow, WorkflowNode, WorkflowEdge, NodeTemplate } from '../types/workflow';

// Custom node component
function DataProcessingNode({ data }: { data: any }) {
  return (
    <div className="px-4 py-2 bg-white border-2 border-indigo-500 rounded-lg shadow-md min-w-[150px]">
      <Handle type="target" position={Position.Left} className="w-3 h-3 bg-indigo-500" />
      <div className="font-medium text-gray-900">数据处理</div>
      <div className="text-xs text-gray-500">{data.label || '未命名节点'}</div>
      {data.config?.expression && (
        <div className="text-xs text-gray-400 mt-1 truncate">{data.config.expression}</div>
      )}
      <Handle type="source" position={Position.Right} className="w-3 h-3 bg-indigo-500" />
    </div>
  );
}

function ConditionNode({ data }: { data: any }) {
  return (
    <div className="px-4 py-2 bg-white border-2 border-amber-500 rounded-lg shadow-md min-w-[150px]">
      <Handle type="target" position={Position.Left} className="w-3 h-3 bg-amber-500" />
      <div className="font-medium text-gray-900">条件分支</div>
      <div className="text-xs text-gray-500">{data.label || '未命名节点'}</div>
      {data.config?.conditions && (
        <div className="text-xs text-gray-400 mt-1">
          {data.config.conditions.length} 个条件
        </div>
      )}
      <Handle type="source" position={Position.Right} className="w-3 h-3 bg-amber-500" />
    </div>
  );
}

function HttpRequestNode({ data }: { data: any }) {
  return (
    <div className="px-4 py-2 bg-white border-2 border-green-500 rounded-lg shadow-md min-w-[150px]">
      <Handle type="target" position={Position.Left} className="w-3 h-3 bg-green-500" />
      <div className="font-medium text-gray-900">HTTP 请求</div>
      <div className="text-xs text-gray-500">{data.label || '未命名节点'}</div>
      {data.config?.url && (
        <div className="text-xs text-gray-400 mt-1 truncate">{data.config.method || 'GET'} {data.config.url}</div>
      )}
      <Handle type="source" position={Position.Right} className="w-3 h-3 bg-green-500" />
    </div>
  );
}

function LlmCallNode({ data }: { data: any }) {
  return (
    <div className="px-4 py-2 bg-white border-2 border-purple-500 rounded-lg shadow-md min-w-[150px]">
      <Handle type="target" position={Position.Left} className="w-3 h-3 bg-purple-500" />
      <div className="font-medium text-gray-900">LLM 调用</div>
      <div className="text-xs text-gray-500">{data.label || '未命名节点'}</div>
      {data.config?.model && (
        <div className="text-xs text-gray-400 mt-1 truncate">{data.config.provider || 'openai'}/{data.config.model}</div>
      )}
      <Handle type="source" position={Position.Right} className="w-3 h-3 bg-purple-500" />
    </div>
  );
}

function ParallelNode({ data }: { data: any }) {
  return (
    <div className="px-4 py-2 bg-white border-2 border-cyan-500 rounded-lg shadow-md min-w-[150px]">
      <Handle type="target" position={Position.Left} className="w-3 h-3 bg-cyan-500" />
      <div className="font-medium text-gray-900">并行执行</div>
      <div className="text-xs text-gray-500">{data.label || '未命名节点'}</div>
      {data.config?.branches && (
        <div className="text-xs text-gray-400 mt-1">{data.config.branches.length} 个分支</div>
      )}
      <Handle type="source" position={Position.Right} className="w-3 h-3 bg-cyan-500" />
    </div>
  );
}

function ForeachNode({ data }: { data: any }) {
  return (
    <div className="px-4 py-2 bg-white border-2 border-pink-500 rounded-lg shadow-md min-w-[150px]">
      <Handle type="target" position={Position.Left} className="w-3 h-3 bg-pink-500" />
      <div className="font-medium text-gray-900">循环迭代</div>
      <div className="text-xs text-gray-500">{data.label || '未命名节点'}</div>
      {data.config?.collection && (
        <div className="text-xs text-gray-400 mt-1 truncate">集合: {data.config.collection}</div>
      )}
      <Handle type="source" position={Position.Right} className="w-3 h-3 bg-pink-500" />
    </div>
  );
}

function BranchNode({ data }: { data: any }) {
  return (
    <div className="px-4 py-2 bg-white border-2 border-orange-500 rounded-lg shadow-md min-w-[150px]">
      <Handle type="target" position={Position.Left} className="w-3 h-3 bg-orange-500" />
      <div className="font-medium text-gray-900">分支</div>
      <div className="text-xs text-gray-500">{data.label || '未命名节点'}</div>
      {data.config?.branches && (
        <div className="text-xs text-gray-400 mt-1">{data.config.branches.length} 个分支</div>
      )}
      <Handle type="source" position={Position.Right} className="w-3 h-3 bg-orange-500" />
    </div>
  );
}

function SubworkflowNode({ data }: { data: any }) {
  return (
    <div className="px-4 py-2 bg-white border-2 border-teal-500 rounded-lg shadow-md min-w-[150px]">
      <Handle type="target" position={Position.Left} className="w-3 h-3 bg-teal-500" />
      <div className="font-medium text-gray-900">子工作流</div>
      <div className="text-xs text-gray-500">{data.label || '未命名节点'}</div>
      {data.config?.workflowId && (
        <div className="text-xs text-gray-400 mt-1 truncate">工作流: {data.config.workflowId}</div>
      )}
      <Handle type="source" position={Position.Right} className="w-3 h-3 bg-teal-500" />
    </div>
  );
}

function TryCatchNode({ data }: { data: any }) {
  return (
    <div className="px-4 py-2 bg-white border-2 border-red-500 rounded-lg shadow-md min-w-[150px]">
      <Handle type="target" position={Position.Left} className="w-3 h-3 bg-red-500" />
      <div className="font-medium text-gray-900">异常捕获</div>
      <div className="text-xs text-gray-500">{data.label || '未命名节点'}</div>
      <Handle type="source" position={Position.Right} className="w-3 h-3 bg-red-500" />
    </div>
  );
}

function RetryNode({ data }: { data: any }) {
  return (
    <div className="px-4 py-2 bg-white border-2 border-yellow-500 rounded-lg shadow-md min-w-[150px]">
      <Handle type="target" position={Position.Left} className="w-3 h-3 bg-yellow-500" />
      <div className="font-medium text-gray-900">重试</div>
      <div className="text-xs text-gray-500">{data.label || '未命名节点'}</div>
      {data.config?.maxAttempts && (
        <div className="text-xs text-gray-400 mt-1">最多 {data.config.maxAttempts} 次</div>
      )}
      <Handle type="source" position={Position.Right} className="w-3 h-3 bg-yellow-500" />
    </div>
  );
}

// LLM Config Panel with template selection
function LLMConfigPanel({ config, onChange }: { config: any; onChange: (c: any) => void }) {
  const { data: templates } = useNodeTemplatesByType('LLM_CALL');

  const handleTemplateChange = (templateId: string) => {
    const template = templates?.find(t => t.id === templateId);
    if (template) {
      onChange({
        ...config,
        provider: template.config.provider,
        model: template.config.model,
        temperature: template.config.temperature,
        maxTokens: template.config.maxTokens,
        outputKey: config.outputKey || 'llmResponse',
      });
    }
  };

  return (
    <>
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">选择模板</label>
        <select
          value=""
          onChange={(e) => handleTemplateChange(e.target.value)}
          className="w-full border rounded-md px-2 py-1 text-sm"
        >
          <option value="">-- 选择预设模板 --</option>
          {templates?.map(template => (
            <option key={template.id} value={template.id}>
              {template.name}
            </option>
          ))}
        </select>
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Provider</label>
        <select
          value={config.provider || 'openai'}
          onChange={(e) => onChange({ ...config, provider: e.target.value })}
          className="w-full border rounded-md px-2 py-1 text-sm"
        >
          <option value="openai">OpenAI</option>
          <option value="anthropic">Anthropic</option>
          <option value="azure">Azure OpenAI</option>
        </select>
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Model</label>
        <input
          type="text"
          value={config.model || 'gpt-4'}
          onChange={(e) => onChange({ ...config, model: e.target.value })}
          className="w-full border rounded-md px-2 py-1 text-sm"
          placeholder="gpt-4"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Temperature</label>
        <input
          type="number"
          step="0.1"
          min="0"
          max="2"
          value={config.temperature ?? 0.7}
          onChange={(e) => onChange({ ...config, temperature: parseFloat(e.target.value) })}
          className="w-full border rounded-md px-2 py-1 text-sm"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Max Tokens</label>
        <input
          type="number"
          value={config.maxTokens || 2000}
          onChange={(e) => onChange({ ...config, maxTokens: parseInt(e.target.value) })}
          className="w-full border rounded-md px-2 py-1 text-sm"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Prompt</label>
        <textarea
          value={config.prompt || ''}
          onChange={(e) => onChange({ ...config, prompt: e.target.value })}
          className="w-full border rounded-md px-2 py-1 text-sm h-24"
          placeholder="Please process #input.value"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">输出 Key</label>
        <input
          type="text"
          value={config.outputKey || 'llmResponse'}
          onChange={(e) => onChange({ ...config, outputKey: e.target.value })}
          className="w-full border rounded-md px-2 py-1 text-sm"
          placeholder="llmResponse"
        />
      </div>
    </>
  );
}

// Node Templates Management Panel
function NodeTemplatesPanel() {
  const { data: templates, isLoading } = useNodeTemplates();
  const [editingTemplate, setEditingTemplate] = useState<NodeTemplate | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState<Partial<NodeTemplate>>({});

  const nodeTypeOptions = [
    { value: 'LLM_CALL', label: 'LLM 调用' },
    { value: 'HTTP_REQUEST', label: 'HTTP 请求' },
  ];

  const handleEdit = (template: NodeTemplate) => {
    setEditingTemplate(template);
    setFormData(template);
    setShowForm(true);
  };

  const handleCreate = () => {
    setEditingTemplate(null);
    setFormData({ name: '', nodeType: 'LLM_CALL', config: {}, active: true });
    setShowForm(true);
  };

  const handleSave = () => {
    // In a real app, this would call an API to save
    setShowForm(false);
    setEditingTemplate(null);
  };

  if (isLoading) {
    return <div className="text-sm text-gray-500">加载中...</div>;
  }

  if (showForm) {
    return (
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h4 className="font-medium text-gray-900">{editingTemplate ? '编辑模板' : '新建模板'}</h4>
          <button onClick={() => setShowForm(false)} className="text-gray-500 hover:text-gray-700">取消</button>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">名称</label>
          <input
            type="text"
            value={formData.name || ''}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            className="w-full border rounded-md px-2 py-1 text-sm"
            placeholder="模板名称"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">节点类型</label>
          <select
            value={formData.nodeType || 'LLM_CALL'}
            onChange={(e) => setFormData({ ...formData, nodeType: e.target.value as any })}
            className="w-full border rounded-md px-2 py-1 text-sm"
          >
            {nodeTypeOptions.map(opt => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Provider</label>
          <select
            value={(formData.config as any)?.provider || 'openai'}
            onChange={(e) => setFormData({ ...formData, config: { ...formData.config, provider: e.target.value } })}
            className="w-full border rounded-md px-2 py-1 text-sm"
          >
            <option value="openai">OpenAI</option>
            <option value="anthropic">Anthropic</option>
            <option value="azure">Azure OpenAI</option>
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Model</label>
          <input
            type="text"
            value={(formData.config as any)?.model || 'gpt-4'}
            onChange={(e) => setFormData({ ...formData, config: { ...formData.config, model: e.target.value } })}
            className="w-full border rounded-md px-2 py-1 text-sm"
            placeholder="gpt-4"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Temperature</label>
          <input
            type="number"
            step="0.1"
            min="0"
            max="2"
            value={(formData.config as any)?.temperature ?? 0.7}
            onChange={(e) => setFormData({ ...formData, config: { ...formData.config, temperature: parseFloat(e.target.value) } })}
            className="w-full border rounded-md px-2 py-1 text-sm"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Max Tokens</label>
          <input
            type="number"
            value={(formData.config as any)?.maxTokens || 2000}
            onChange={(e) => setFormData({ ...formData, config: { ...formData.config, maxTokens: parseInt(e.target.value) } })}
            className="w-full border rounded-md px-2 py-1 text-sm"
          />
        </div>
        <div className="flex items-center">
          <input
            type="checkbox"
            id="active"
            checked={formData.active ?? true}
            onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
            className="w-4 h-4 mr-2"
          />
          <label htmlFor="active" className="text-sm text-gray-700">启用</label>
        </div>
        <button
          onClick={handleSave}
          className="w-full px-3 py-1.5 bg-indigo-600 text-white rounded-md text-sm hover:bg-indigo-700"
        >
          保存
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h4 className="font-medium text-gray-900">模板列表</h4>
        <button
          onClick={handleCreate}
          className="px-2 py-1 bg-indigo-100 text-indigo-700 rounded text-xs hover:bg-indigo-200"
        >
          + 新建
        </button>
      </div>
      <div className="space-y-2 max-h-64 overflow-y-auto">
        {templates?.map(template => (
          <div key={template.id} className="border rounded-md p-2 text-sm">
            <div className="flex items-center justify-between">
              <span className="font-medium">{template.name}</span>
              <span className={`text-xs px-1.5 py-0.5 rounded ${template.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                {template.active ? '启用' : '禁用'}
              </span>
            </div>
            <div className="text-xs text-gray-500 mt-1">
              {template.nodeType === 'LLM_CALL' && `${(template.config as any)?.provider || 'openai'}/${(template.config as any)?.model || 'gpt-4'}`}
              {template.nodeType === 'HTTP_REQUEST' && (template.config as any)?.url}
            </div>
            <button
              onClick={() => handleEdit(template)}
              className="mt-1 text-xs text-indigo-600 hover:text-indigo-800"
            >
              编辑
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}

// Node Type List Panel (shown when no node selected)
function NodeTypeListPanel({ onAddNode }: { onAddNode: (type: string) => void }) {
  const nodeTypes = [
    { type: 'DATA_PROCESSING', label: '数据处理', color: 'bg-indigo-100 text-indigo-700', icon: '⚙️' },
    { type: 'CONDITION', label: '条件分支', color: 'bg-amber-100 text-amber-700', icon: '🔀' },
    { type: 'HTTP_REQUEST', label: 'HTTP 请求', color: 'bg-green-100 text-green-700', icon: '🌐' },
    { type: 'LLM_CALL', label: 'LLM 调用', color: 'bg-purple-100 text-purple-700', icon: '🤖' },
    { type: 'PARALLEL', label: '并行执行', color: 'bg-cyan-100 text-cyan-700', icon: '⚡' },
    { type: 'FOREACH', label: '循环迭代', color: 'bg-pink-100 text-pink-700', icon: '🔄' },
    { type: 'BRANCH', label: '分支', color: 'bg-orange-100 text-orange-700', icon: '🌳' },
    { type: 'SUBWORKFLOW', label: '子工作流', color: 'bg-teal-100 text-teal-700', icon: '📦' },
    { type: 'TRY_CATCH', label: '异常捕获', color: 'bg-red-100 text-red-700', icon: '🛡️' },
    { type: 'RETRY', label: '重试', color: 'bg-yellow-100 text-yellow-700', icon: '🔁' },
  ];

  return (
    <div className="space-y-2">
      <h4 className="font-medium text-gray-900 text-sm">添加节点</h4>
      <div className="grid grid-cols-2 gap-2">
        {nodeTypes.map(({ type, label, color, icon }) => (
          <button
            key={type}
            onClick={() => onAddNode(type)}
            className={`${color} px-3 py-2 rounded-md text-sm text-left hover:opacity-80 transition-opacity`}
          >
            <div className="flex items-center space-x-1.5">
              <span>{icon}</span>
              <span>{label}</span>
            </div>
          </button>
        ))}
      </div>
    </div>
  );
}

const nodeTypes: NodeTypes = {
  DATA_PROCESSING: DataProcessingNode,
  CONDITION: ConditionNode,
  HTTP_REQUEST: HttpRequestNode,
  LLM_CALL: LlmCallNode,
  PARALLEL: ParallelNode,
  FOREACH: ForeachNode,
  BRANCH: BranchNode,
  SUBWORKFLOW: SubworkflowNode,
  TRY_CATCH: TryCatchNode,
  RETRY: RetryNode,
};

const defaultNodes: Node[] = [
  {
    id: 'start',
    type: 'DATA_PROCESSING',
    position: { x: 100, y: 200 },
    data: { label: '开始节点', type: 'DATA_PROCESSING', config: { outputKey: 'result', expression: '#input.value' } },
  },
];

const defaultEdges: Edge[] = [];

export function WorkflowEditorPage() {
  const [searchParams] = useSearchParams();
  const workflowId = searchParams.get('id');
  const navigate = useNavigate();
  const { data: existingWorkflow } = useWorkflow(workflowId || '');
  const { data: workflows } = useWorkflows();
  const createWorkflow = useCreateWorkflow();

  const [nodes, setNodes, onNodesChange] = useNodesState(defaultNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(defaultEdges);
  const [workflowName, setWorkflowName] = useState('新工作流');
  const [selectedNode, setSelectedNode] = useState<Node | null>(null);
  const [nodeConfig, setNodeConfig] = useState<Record<string, any>>({});
  const [saveWarning, setSaveWarning] = useState<string | null>(null);
  const [rightTab, setRightTab] = useState<'config' | 'templates'>('config');
  const [justSaved, setJustSaved] = useState(false);

  // Load existing workflow if editing
  useMemo(() => {
    if (existingWorkflow) {
      setWorkflowName(existingWorkflow.name);
      const loadedNodes: Node[] = Object.entries(existingWorkflow.nodes || {}).map(([id, node], index) => ({
        id,
        type: (node as any).type || 'DATA_PROCESSING',
        position: { x: 150 + index * 250, y: 200 },
        data: { ...(node as any), label: (node as any).name || id },
      }));
      setNodes(loadedNodes);
      const loadedEdges: Edge[] = (existingWorkflow.edges || []).map((e: WorkflowEdge) => ({
        id: e.id,
        source: e.fromNodeId,
        target: e.toNodeId,
        animated: true,
      }));
      setEdges(loadedEdges);
    }
  }, [existingWorkflow, setNodes, setEdges]);

  const onConnect = useCallback(
    (params: Connection) =>
      setEdges((eds) =>
        addEdge(
          {
            ...params,
            animated: true,
            id: `edge-${Date.now()}`,
          },
          eds
        )
      ),
    [setEdges]
  );

  const onNodeClick = useCallback((_event: React.MouseEvent, node: Node) => {
    setSelectedNode(node);
    setNodeConfig((node.data as any)?.config || {});
    setJustSaved(false);
  }, []);

  const addNode = (type: string) => {
    const typeLabels: Record<string, string> = {
      DATA_PROCESSING: '新数据节点',
      CONDITION: '新条件节点',
      HTTP_REQUEST: '新HTTP请求',
      LLM_CALL: '新LLM调用',
      PARALLEL: '新并行节点',
      FOREACH: '新循环节点',
      BRANCH: '新分支节点',
      SUBWORKFLOW: '新子工作流',
      TRY_CATCH: '新异常捕获',
      RETRY: '新重试节点',
    };

    const defaultConfigs: Record<string, Record<string, unknown>> = {
      DATA_PROCESSING: { outputKey: 'result', expression: '#input.value' },
      CONDITION: { conditions: [], defaultNextNodeId: '' },
      HTTP_REQUEST: { url: 'https://api.example.com', method: 'GET', outputKey: 'httpResponse' },
      LLM_CALL: { provider: 'openai', model: 'gpt-4', prompt: 'Please process this input', outputKey: 'llmResponse' },
      PARALLEL: { branches: [], strategy: 'AND' },
      FOREACH: { collection: '#input.items', variableName: 'item', subgraphNodeIds: [] },
      BRANCH: { branches: [], defaultNextNodeId: '' },
      SUBWORKFLOW: { workflowId: '', nextNodeId: '' },
      TRY_CATCH: { tryNodes: [], catchNodes: [] },
      RETRY: { maxAttempts: 3, initialDelayMs: 1000, backoffStrategy: 'EXPONENTIAL', targetNodeIds: [] },
    };

    const newNode: Node = {
      id: `${type}-${Date.now()}`,
      type,
      position: { x: Math.random() * 400 + 100, y: Math.random() * 300 + 100 },
      data: {
        label: typeLabels[type] || '新节点',
        type,
        config: defaultConfigs[type] || {},
      },
    };
    setNodes((nds) => [...nds, newNode]);
    setJustSaved(false);
  };

  const updateSelectedNode = () => {
    if (!selectedNode) return;
    setNodes((nds) =>
      nds.map((n) =>
        n.id === selectedNode.id
          ? { ...n, data: { ...n.data, config: nodeConfig } }
          : n
      )
    );
    setSelectedNode(null);
  };

  const deleteSelectedNode = () => {
    if (!selectedNode) return;
    setNodes((nds) => nds.filter((n) => n.id !== selectedNode.id));
    setEdges((eds) => eds.filter((e) => e.source !== selectedNode.id && e.target !== selectedNode.id));
    setSelectedNode(null);
  };

  const saveWorkflow = async () => {
    // Check for duplicate workflow name (exclude current workflow if editing)
    const isDuplicate = workflows?.some(w =>
      w.name === workflowName && w.id !== workflowId
    );

    if (isDuplicate) {
      setSaveWarning(`工作流名称 "${workflowName}" 已存在，请使用其他名称`);
      return;
    }

    const workflowNodes: Record<string, WorkflowNode> = {};
    nodes.forEach((node) => {
      workflowNodes[node.id] = {
        id: node.id,
        name: (node.data as any)?.label || node.id,
        type: (node.data as any)?.type || 'DATA_PROCESSING',
        config: (node.data as any)?.config || {},
        inputMapping: {},
        outputMapping: {},
      };
    });

    const workflowEdges: WorkflowEdge[] = edges.map((edge) => ({
      id: edge.id,
      fromNodeId: edge.source as string,
      toNodeId: edge.target as string,
    }));

    const startNodeId = nodes.length > 0 ? nodes[0].id : '';

    const workflow: Workflow = {
      id: workflowId || `wf-${Date.now()}`,
      name: workflowName,
      startNodeId,
      nodes: workflowNodes,
      edges: workflowEdges,
    };

    await createWorkflow.mutateAsync(workflow);
    setJustSaved(true);
    setSelectedNode(null);
    navigate('/');
  };

  const handleLabelChange = (e: ChangeEvent<HTMLInputElement>) => {
    if (!selectedNode) return;
    setNodes((nds) =>
      nds.map((n) =>
        n.id === selectedNode.id
          ? { ...n, data: { ...n.data, label: e.target.value } }
          : n
      )
    );
  };

  return (
    <div className="h-[calc(100vh-120px)] flex flex-col">
      {/* Toolbar */}
      <div className="bg-white border-b px-4 py-3 flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <input
            type="text"
            value={workflowName}
            onChange={(e) => { setWorkflowName(e.target.value); setSaveWarning(null); }}
            className="text-xl font-bold border-b-2 border-indigo-500 bg-transparent px-2 py-1 focus:outline-none"
            placeholder="工作流名称"
          />
          {saveWarning && (
            <span className="text-sm text-amber-600">{saveWarning}</span>
          )}
        </div>
        <div className="flex items-center space-x-2">
          <button
            onClick={saveWorkflow}
            className="px-4 py-1.5 bg-indigo-600 text-white rounded-md text-sm hover:bg-indigo-700"
          >
            保存
          </button>
        </div>
      </div>

      {/* Canvas */}
      <div className="flex-1 relative flex">
        <div className="flex-1 relative">
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onNodeClick={onNodeClick}
            nodeTypes={nodeTypes}
            fitView
            className="bg-gray-50"
          >
            <Controls />
            <MiniMap />
            <Background variant={BackgroundVariant.Dots} gap={20} size={1} />
          </ReactFlow>
        </div>

        {/* Right Panel with Tabs */}
        <div className="w-80 bg-white border-l flex flex-col">
          {/* Tabs */}
          <div className="flex border-b">
            <button
              onClick={() => setRightTab('config')}
              className={`flex-1 px-4 py-2 text-sm font-medium ${
                rightTab === 'config'
                  ? 'text-indigo-600 border-b-2 border-indigo-600'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              节点配置
            </button>
            <button
              onClick={() => setRightTab('templates')}
              className={`flex-1 px-4 py-2 text-sm font-medium ${
                rightTab === 'templates'
                  ? 'text-indigo-600 border-b-2 border-indigo-600'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              模板管理
            </button>
          </div>

          {/* Tab Content */}
          <div className="flex-1 overflow-y-auto p-4">
            {rightTab === 'templates' ? (
              <NodeTemplatesPanel />
            ) : selectedNode ? (
              /* Node Config Panel when node selected */
              <div className="space-y-3">
                <h3 className="font-bold text-gray-900">节点配置</h3>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">节点名称</label>
                  <input
                    type="text"
                    value={(selectedNode.data as any)?.label || ''}
                    onChange={handleLabelChange}
                    className="w-full border rounded-md px-2 py-1 text-sm"
                  />
                </div>
                {selectedNode.type === 'DATA_PROCESSING' && (
                  <>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">表达式</label>
                      <input
                        type="text"
                        value={nodeConfig.expression || ''}
                        onChange={(e) => setNodeConfig({ ...nodeConfig, expression: e.target.value })}
                        className="w-full border rounded-md px-2 py-1 text-sm"
                        placeholder="#input.value * 2"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">输出 Key</label>
                      <input
                        type="text"
                        value={nodeConfig.outputKey || ''}
                        onChange={(e) => setNodeConfig({ ...nodeConfig, outputKey: e.target.value })}
                        className="w-full border rounded-md px-2 py-1 text-sm"
                        placeholder="result"
                      />
                    </div>
                  </>
                )}
                {selectedNode.type === 'CONDITION' && (
                  <>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">条件 (JSON)</label>
                      <textarea
                        value={JSON.stringify(nodeConfig.conditions || [], null, 2)}
                        onChange={(e) => {
                          try {
                            setNodeConfig({ ...nodeConfig, conditions: JSON.parse(e.target.value) });
                          } catch {}
                        }}
                        className="w-full border rounded-md px-2 py-1 text-sm h-24"
                        placeholder='[{"expression": "#input.value > 100", "nextNodeId": "node1"}]'
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">默认下一节点</label>
                      <input
                        type="text"
                        value={nodeConfig.defaultNextNodeId || ''}
                        onChange={(e) => setNodeConfig({ ...nodeConfig, defaultNextNodeId: e.target.value })}
                        className="w-full border rounded-md px-2 py-1 text-sm"
                        placeholder="default-node"
                      />
                    </div>
                  </>
                )}
                {selectedNode.type === 'HTTP_REQUEST' && (
                  <>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">URL</label>
                      <input
                        type="text"
                        value={nodeConfig.url || ''}
                        onChange={(e) => setNodeConfig({ ...nodeConfig, url: e.target.value })}
                        className="w-full border rounded-md px-2 py-1 text-sm"
                        placeholder="https://api.example.com"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Method</label>
                      <select
                        value={nodeConfig.method || 'GET'}
                        onChange={(e) => setNodeConfig({ ...nodeConfig, method: e.target.value })}
                        className="w-full border rounded-md px-2 py-1 text-sm"
                      >
                        <option value="GET">GET</option>
                        <option value="POST">POST</option>
                        <option value="PUT">PUT</option>
                        <option value="DELETE">DELETE</option>
                        <option value="PATCH">PATCH</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Body (JSON)</label>
                      <textarea
                        value={typeof nodeConfig.body === 'string' ? nodeConfig.body : JSON.stringify(nodeConfig.body || {}, null, 2)}
                        onChange={(e) => {
                          try {
                            setNodeConfig({ ...nodeConfig, body: JSON.parse(e.target.value) });
                          } catch {
                            setNodeConfig({ ...nodeConfig, body: e.target.value });
                          }
                        }}
                        className="w-full border rounded-md px-2 py-1 text-sm h-20"
                        placeholder='{"key": "value"}'
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">输出 Key</label>
                      <input
                        type="text"
                        value={nodeConfig.outputKey || 'httpResponse'}
                        onChange={(e) => setNodeConfig({ ...nodeConfig, outputKey: e.target.value })}
                        className="w-full border rounded-md px-2 py-1 text-sm"
                        placeholder="httpResponse"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">超时 (ms)</label>
                      <input
                        type="number"
                        value={nodeConfig.timeout || 30000}
                        onChange={(e) => setNodeConfig({ ...nodeConfig, timeout: parseInt(e.target.value) })}
                        className="w-full border rounded-md px-2 py-1 text-sm"
                      />
                    </div>
                  </>
                )}
                {selectedNode.type === 'LLM_CALL' && (
                  <LLMConfigPanel
                    config={nodeConfig}
                    onChange={setNodeConfig}
                  />
                )}
                {selectedNode.type === 'PARALLEL' && (
                  <>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">策略</label>
                      <select
                        value={nodeConfig.strategy || 'AND'}
                        onChange={(e) => setNodeConfig({ ...nodeConfig, strategy: e.target.value })}
                        className="w-full border rounded-md px-2 py-1 text-sm"
                      >
                        <option value="AND">AND (全部完成)</option>
                        <option value="OR">OR (任一完成)</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">分支 (JSON)</label>
                      <textarea
                        value={JSON.stringify(nodeConfig.branches || [], null, 2)}
                        onChange={(e) => {
                          try {
                            setNodeConfig({ ...nodeConfig, branches: JSON.parse(e.target.value) });
                          } catch {}
                        }}
                        className="w-full border rounded-md px-2 py-1 text-sm h-32"
                        placeholder='[{"name": "branch1", "nodeIds": ["n1", "n2"]}]'
                      />
                    </div>
                  </>
                )}
                {selectedNode.type === 'FOREACH' && (
                  <>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">集合表达式</label>
                      <input
                        type="text"
                        value={nodeConfig.collection || '#input.items'}
                        onChange={(e) => setNodeConfig({ ...nodeConfig, collection: e.target.value })}
                        className="w-full border rounded-md px-2 py-1 text-sm"
                        placeholder="#input.items"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">循环变量名</label>
                      <input
                        type="text"
                        value={nodeConfig.variableName || 'item'}
                        onChange={(e) => setNodeConfig({ ...nodeConfig, variableName: e.target.value })}
                        className="w-full border rounded-md px-2 py-1 text-sm"
                        placeholder="item"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">最大迭代次数</label>
                      <input
                        type="number"
                        value={nodeConfig.maxIterations || 100}
                        onChange={(e) => setNodeConfig({ ...nodeConfig, maxIterations: parseInt(e.target.value) })}
                        className="w-full border rounded-md px-2 py-1 text-sm"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">失败时终止</label>
                      <input
                        type="checkbox"
                        checked={nodeConfig.failOnError !== false}
                        onChange={(e) => setNodeConfig({ ...nodeConfig, failOnError: e.target.checked })}
                        className="w-4 h-4"
                      />
                    </div>
                  </>
                )}
                {selectedNode.type === 'BRANCH' && (
                  <>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">分支 (JSON)</label>
                      <textarea
                        value={JSON.stringify(nodeConfig.branches || [], null, 2)}
                        onChange={(e) => {
                          try {
                            setNodeConfig({ ...nodeConfig, branches: JSON.parse(e.target.value) });
                          } catch {}
                        }}
                        className="w-full border rounded-md px-2 py-1 text-sm h-32"
                        placeholder='[{"name": "high", "expression": "#input.value > 100", "nextNodeId": "n1"}]'
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">默认下一节点</label>
                      <input
                        type="text"
                        value={nodeConfig.defaultNextNodeId || ''}
                        onChange={(e) => setNodeConfig({ ...nodeConfig, defaultNextNodeId: e.target.value })}
                        className="w-full border rounded-md px-2 py-1 text-sm"
                        placeholder="default-node"
                      />
                    </div>
                  </>
                )}
                {selectedNode.type === 'SUBWORKFLOW' && (
                  <>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">工作流 ID</label>
                      <input
                        type="text"
                        value={nodeConfig.workflowId || ''}
                        onChange={(e) => setNodeConfig({ ...nodeConfig, workflowId: e.target.value })}
                        className="w-full border rounded-md px-2 py-1 text-sm"
                        placeholder="sub-workflow-id"
                      />
                    </div>
                  </>
                )}
                {selectedNode.type === 'TRY_CATCH' && (
                  <>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Try 节点 (JSON)</label>
                      <textarea
                        value={JSON.stringify(nodeConfig.tryNodes || [], null, 2)}
                        onChange={(e) => {
                          try {
                            setNodeConfig({ ...nodeConfig, tryNodes: JSON.parse(e.target.value) });
                          } catch {}
                        }}
                        className="w-full border rounded-md px-2 py-1 text-sm h-24"
                        placeholder='["node1", "node2"]'
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Catch 节点 (JSON)</label>
                      <textarea
                        value={JSON.stringify(nodeConfig.catchNodes || [], null, 2)}
                        onChange={(e) => {
                          try {
                            setNodeConfig({ ...nodeConfig, catchNodes: JSON.parse(e.target.value) });
                          } catch {}
                        }}
                        className="w-full border rounded-md px-2 py-1 text-sm h-24"
                        placeholder='["error_handler"]'
                      />
                    </div>
                  </>
                )}
                {selectedNode.type === 'RETRY' && (
                  <>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">最大重试次数</label>
                      <input
                        type="number"
                        value={nodeConfig.maxAttempts || 3}
                        onChange={(e) => setNodeConfig({ ...nodeConfig, maxAttempts: parseInt(e.target.value) })}
                        className="w-full border rounded-md px-2 py-1 text-sm"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">初始延迟 (ms)</label>
                      <input
                        type="number"
                        value={nodeConfig.initialDelayMs || 1000}
                        onChange={(e) => setNodeConfig({ ...nodeConfig, initialDelayMs: parseInt(e.target.value) })}
                        className="w-full border rounded-md px-2 py-1 text-sm"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">最大延迟 (ms)</label>
                      <input
                        type="number"
                        value={nodeConfig.maxDelayMs || 30000}
                        onChange={(e) => setNodeConfig({ ...nodeConfig, maxDelayMs: parseInt(e.target.value) })}
                        className="w-full border rounded-md px-2 py-1 text-sm"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">退避策略</label>
                      <select
                        value={nodeConfig.backoffStrategy || 'EXPONENTIAL'}
                        onChange={(e) => setNodeConfig({ ...nodeConfig, backoffStrategy: e.target.value })}
                        className="w-full border rounded-md px-2 py-1 text-sm"
                      >
                        <option value="FIXED">固定</option>
                        <option value="EXPONENTIAL">指数</option>
                        <option value="FIBONACCI">斐波那契</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">目标节点 (JSON)</label>
                      <textarea
                        value={JSON.stringify(nodeConfig.targetNodeIds || [], null, 2)}
                        onChange={(e) => {
                          try {
                            setNodeConfig({ ...nodeConfig, targetNodeIds: JSON.parse(e.target.value) });
                          } catch {}
                        }}
                        className="w-full border rounded-md px-2 py-1 text-sm h-20"
                        placeholder='["node1"]'
                      />
                    </div>
                  </>
                )}
                <div className="flex space-x-2 pt-2">
                  <button
                    onClick={updateSelectedNode}
                    className="flex-1 px-3 py-1.5 bg-indigo-600 text-white rounded-md text-sm hover:bg-indigo-700"
                  >
                    应用
                  </button>
                  <button
                    onClick={deleteSelectedNode}
                    className="px-3 py-1.5 bg-red-100 text-red-700 rounded-md text-sm hover:bg-red-200"
                  >
                    删除
                  </button>
                </div>
              </div>
            ) : (
              /* Node Type List when no node selected */
              <div>
                {justSaved && (
                  <div className="mb-4 p-2 bg-green-50 text-green-700 text-sm rounded-md">
                    保存成功！请从下方添加节点
                  </div>
                )}
                <div className="text-sm text-gray-500 mb-4">
                  点击画布上的节点进行配置，或从下方选择添加新节点
                </div>
                <NodeTypeListPanel onAddNode={addNode} />
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
