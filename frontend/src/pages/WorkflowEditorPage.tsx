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
import { useCreateWorkflow, useWorkflow, useWorkflows, useNodeTemplatesByType } from '../hooks/useWorkflow';
import { useNavigate, useSearchParams } from 'react-router-dom';
import type { Workflow, WorkflowNode, WorkflowEdge, NodeTemplate } from '../types/workflow';
import StartNode from '../components/nodes/StartNode';

// Helper to format degree display
function DegreeBadge({ current, max, type }: { current: number; max: number | null; type: 'in' | 'out' }) {
  const maxDisplay = max === null ? '∞' : max;
  const isLimited = max !== null;
  const isFull = isLimited && current >= max;
  const colorClass = isFull ? 'bg-red-100 text-red-700' : 'bg-gray-100 text-gray-600';
  return (
    <span className={`text-xs px-1.5 py-0.5 rounded ${colorClass}`} title={type === 'in' ? '入度' : '出度'}>
      {type === 'in' ? '入' : '出'}:{current}/{maxDisplay}
    </span>
  );
}

// Custom node component with degree display
function DataProcessingNode({ data }: { data: any }) {
  return (
    <div className="px-4 py-2 bg-white border-2 border-indigo-500 rounded-lg shadow-md min-w-[150px]">
      <Handle type="target" position={Position.Left} className="w-3 h-3 bg-indigo-500" />
      <div className="flex items-center justify-between">
        <div className="font-medium text-gray-900">数据处理</div>
        {data.showDegree && (
          <div className="flex gap-1">
            <DegreeBadge current={data.inDegree || 0} max={data.maxIn} type="in" />
            <DegreeBadge current={data.outDegree || 0} max={data.maxOut} type="out" />
          </div>
        )}
      </div>
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
      <div className="flex items-center justify-between">
        <div className="font-medium text-gray-900">条件分支</div>
        {data.showDegree && (
          <div className="flex gap-1">
            <DegreeBadge current={data.inDegree || 0} max={data.maxIn} type="in" />
            <DegreeBadge current={data.outDegree || 0} max={data.maxOut} type="out" />
          </div>
        )}
      </div>
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
      <div className="flex items-center justify-between">
        <div className="font-medium text-gray-900">HTTP 请求</div>
        {data.showDegree && (
          <div className="flex gap-1">
            <DegreeBadge current={data.inDegree || 0} max={data.maxIn} type="in" />
            <DegreeBadge current={data.outDegree || 0} max={data.maxOut} type="out" />
          </div>
        )}
      </div>
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
      <div className="flex items-center justify-between">
        <div className="font-medium text-gray-900">LLM 调用</div>
        {data.showDegree && (
          <div className="flex gap-1">
            <DegreeBadge current={data.inDegree || 0} max={data.maxIn} type="in" />
            <DegreeBadge current={data.outDegree || 0} max={data.maxOut} type="out" />
          </div>
        )}
      </div>
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
      <div className="flex items-center justify-between">
        <div className="font-medium text-gray-900">并行执行</div>
        {data.showDegree && (
          <div className="flex gap-1">
            <DegreeBadge current={data.inDegree || 0} max={data.maxIn} type="in" />
            <DegreeBadge current={data.outDegree || 0} max={data.maxOut} type="out" />
          </div>
        )}
      </div>
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
      <div className="flex items-center justify-between">
        <div className="font-medium text-gray-900">循环迭代</div>
        {data.showDegree && (
          <div className="flex gap-1">
            <DegreeBadge current={data.inDegree || 0} max={data.maxIn} type="in" />
            <DegreeBadge current={data.outDegree || 0} max={data.maxOut} type="out" />
          </div>
        )}
      </div>
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
      <div className="flex items-center justify-between">
        <div className="font-medium text-gray-900">分支</div>
        {data.showDegree && (
          <div className="flex gap-1">
            <DegreeBadge current={data.inDegree || 0} max={data.maxIn} type="in" />
            <DegreeBadge current={data.outDegree || 0} max={data.maxOut} type="out" />
          </div>
        )}
      </div>
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
      <div className="flex items-center justify-between">
        <div className="font-medium text-gray-900">子工作流</div>
        {data.showDegree && (
          <div className="flex gap-1">
            <DegreeBadge current={data.inDegree || 0} max={data.maxIn} type="in" />
            <DegreeBadge current={data.outDegree || 0} max={data.maxOut} type="out" />
          </div>
        )}
      </div>
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
      <div className="flex items-center justify-between">
        <div className="font-medium text-gray-900">异常捕获</div>
        {data.showDegree && (
          <div className="flex gap-1">
            <DegreeBadge current={data.inDegree || 0} max={data.maxIn} type="in" />
            <DegreeBadge current={data.outDegree || 0} max={data.maxOut} type="out" />
          </div>
        )}
      </div>
      <div className="text-xs text-gray-500">{data.label || '未命名节点'}</div>
      <Handle type="source" position={Position.Right} className="w-3 h-3 bg-red-500" />
    </div>
  );
}

function RetryNode({ data }: { data: any }) {
  return (
    <div className="px-4 py-2 bg-white border-2 border-yellow-500 rounded-lg shadow-md min-w-[150px]">
      <Handle type="target" position={Position.Left} className="w-3 h-3 bg-yellow-500" />
      <div className="flex items-center justify-between">
        <div className="font-medium text-gray-900">重试</div>
        {data.showDegree && (
          <div className="flex gap-1">
            <DegreeBadge current={data.inDegree || 0} max={data.maxIn} type="in" />
            <DegreeBadge current={data.outDegree || 0} max={data.maxOut} type="out" />
          </div>
        )}
      </div>
      <div className="text-xs text-gray-500">{data.label || '未命名节点'}</div>
      {data.config?.maxAttempts && (
        <div className="text-xs text-gray-400 mt-1">最多 {data.config.maxAttempts} 次</div>
      )}
      <Handle type="source" position={Position.Right} className="w-3 h-3 bg-yellow-500" />
    </div>
  );
}

function EndNode({ data }: { data: any }) {
  return (
    <div className="px-4 py-2 bg-white border-2 border-red-600 rounded-lg shadow-md min-w-[150px]">
      <Handle type="target" position={Position.Left} className="w-3 h-3 bg-red-600" />
      <div className="flex items-center justify-between">
        <div className="font-medium text-gray-900">结束</div>
        {data.showDegree && (
          <div className="flex gap-1">
            <DegreeBadge current={data.inDegree || 0} max={data.maxIn} type="in" />
            <DegreeBadge current={data.outDegree || 0} max={data.maxOut} type="out" />
          </div>
        )}
      </div>
      <div className="text-xs text-gray-500">{data.label || '结束节点'}</div>
    </div>
  );
}

function JoinNode({ data }: { data: any }) {
  return (
    <div className="px-4 py-2 bg-white border-2 border-violet-600 rounded-lg shadow-md min-w-[150px]">
      <Handle type="target" position={Position.Left} className="w-3 h-3 bg-violet-600" />
      <div className="flex items-center justify-between">
        <div className="font-medium text-gray-900">JOIN</div>
        {data.showDegree && (
          <div className="flex gap-1">
            <DegreeBadge current={data.inDegree || 0} max={data.maxIn} type="in" />
            <DegreeBadge current={data.outDegree || 0} max={data.maxOut} type="out" />
          </div>
        )}
      </div>
      <div className="text-xs text-gray-500">{data.label || '汇聚节点'}</div>
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

// Node Templates Modal - for selecting template to fill node config
function TemplateSelectModal({
  nodeType,
  onSelect,
  onClose,
}: {
  nodeType: string;
  onSelect: (template: NodeTemplate) => void;
  onClose: () => void;
}) {
  const { data: templates, isLoading } = useNodeTemplatesByType(nodeType);

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-96 max-h-[80vh] flex flex-col">
        <div className="flex items-center justify-between px-4 py-3 border-b">
          <h3 className="font-bold text-gray-900">选择模板</h3>
          <button onClick={onClose} className="text-gray-500 hover:text-gray-700 text-xl">&times;</button>
        </div>
        <div className="flex-1 overflow-y-auto p-4">
          {isLoading ? (
            <div className="text-center text-gray-500 py-4">加载中...</div>
          ) : !templates || templates.length === 0 ? (
            <div className="text-center text-gray-500 py-4">
              暂无{nodeType}类型的模板<br />
              <span className="text-sm">请到「节点配置」页面创建</span>
            </div>
          ) : (
            <div className="space-y-2">
              {templates.map(template => (
                <button
                  key={template.id}
                  onClick={() => onSelect(template)}
                  className="w-full border rounded-lg p-3 text-left hover:bg-gray-50 hover:shadow-md transition-all"
                >
                  <div className="flex items-center justify-between">
                    <span className="font-medium text-gray-900">{template.name}</span>
                    <span className={`text-xs px-1.5 py-0.5 rounded ${template.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                      {template.active ? '启用' : '禁用'}
                    </span>
                  </div>
                  {template.remark && (
                    <p className="text-xs text-gray-500 mt-1">{template.remark}</p>
                  )}
                  <div className="text-xs text-gray-400 mt-1">
                    {template.nodeType === 'LLM_CALL' && (
                      <span>{(template.config as any)?.provider || 'openai'} / {(template.config as any)?.model || 'gpt-4'}</span>
                    )}
                    {template.nodeType === 'HTTP_REQUEST' && (
                      <span>{(template.config as any)?.method || 'GET'} {(template.config as any)?.url}</span>
                    )}
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// Node Type List Panel (shown when no node selected)
function NodeTypeListPanel({ onAddNode }: { onAddNode: (type: string) => void }) {
  const [openSections, setOpenSections] = useState<Record<string, boolean>>({
    '触发类': true,
    '数据处理': true,
    '集成': false,
    '控制流': false,
    '子工作流': false,
  });

  const toggleSection = (section: string) => {
    setOpenSections((prev) => ({ ...prev, [section]: !prev[section] }));
  };

  const nodeGroups = [
    {
      category: '触发类',
      nodes: [
        { type: 'start', label: '开始', color: 'bg-green-100 text-green-700', icon: '▶️' },
      ],
    },
    {
      category: '结束类',
      nodes: [
        { type: 'END', label: '结束', color: 'bg-red-100 text-red-700', icon: '⏹️' },
      ],
    },
    {
      category: '数据处理',
      nodes: [
        { type: 'DATA_PROCESSING', label: '数据处理', color: 'bg-indigo-100 text-indigo-700', icon: '⚙️' },
        { type: 'CONDITION', label: '条件分支', color: 'bg-amber-100 text-amber-700', icon: '🔀' },
      ],
    },
    {
      category: '集成',
      nodes: [
        { type: 'HTTP_REQUEST', label: 'HTTP 请求', color: 'bg-green-100 text-green-700', icon: '🌐' },
        { type: 'LLM_CALL', label: 'LLM 调用', color: 'bg-purple-100 text-purple-700', icon: '🤖' },
      ],
    },
    {
      category: '控制流',
      nodes: [
        { type: 'PARALLEL', label: '并行执行', color: 'bg-cyan-100 text-cyan-700', icon: '⚡' },
        { type: 'FOREACH', label: '循环迭代', color: 'bg-pink-100 text-pink-700', icon: '🔄' },
        { type: 'BRANCH', label: '分支', color: 'bg-orange-100 text-orange-700', icon: '🌳' },
        { type: 'TRY_CATCH', label: '异常捕获', color: 'bg-red-100 text-red-700', icon: '🛡️' },
        { type: 'RETRY', label: '重试', color: 'bg-yellow-100 text-yellow-700', icon: '🔁' },
        { type: 'JOIN', label: '汇聚', color: 'bg-violet-100 text-violet-700', icon: '🔗' },
      ],
    },
    {
      category: '子工作流',
      nodes: [
        { type: 'SUBWORKFLOW', label: '子工作流', color: 'bg-teal-100 text-teal-700', icon: '📦' },
      ],
    },
  ];

  return (
    <div className="space-y-2">
      <h4 className="font-medium text-gray-900 text-sm">添加节点</h4>
      <div className="space-y-1">
        {nodeGroups.map(({ category, nodes }) => (
          <div key={category} className="border rounded-md overflow-hidden">
            <button
              onClick={() => toggleSection(category)}
              className="w-full px-3 py-2 bg-gray-50 text-left flex items-center justify-between hover:bg-gray-100 transition-colors"
            >
              <span className="font-medium text-gray-700 text-sm">{category}</span>
              <span className="text-gray-400">{openSections[category] ? '▼' : '▶'}</span>
            </button>
            {openSections[category] && (
              <div className="p-2 grid grid-cols-2 gap-2">
                {nodes.map(({ type, label, color, icon }) => (
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
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

// Execute Panel for node testing
function ExecutePanel({
  selectedNode,
  workflowId,
}: {
  selectedNode: Node | null;
  workflowId: string;
}) {
  const [context, setContext] = useState('{"input": {}}');
  const [result, setResult] = useState<any>(null);
  const [isExecuting, setIsExecuting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleExecute = async () => {
    if (!selectedNode || !workflowId) return;

    setIsExecuting(true);
    setError(null);
    setResult(null);

    try {
      const response = await fetch(`/api/workflows/${workflowId}/nodes/${selectedNode.id}/test`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: context,
      });

      if (!response.ok) {
        throw new Error(`执行失败: ${response.statusText}`);
      }

      const data = await response.json();
      setResult(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : '执行失败');
    } finally {
      setIsExecuting(false);
    }
  };

  if (!selectedNode) {
    return (
      <div className="text-sm text-gray-500">
        请先在画布上选择一个节点进行测试
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div>
        <h4 className="font-medium text-gray-900 mb-2">节点执行测试</h4>
        <div className="text-sm text-gray-500 mb-3">
          节点: <span className="font-mono text-gray-700">{selectedNode.id}</span>
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Context (JSON)</label>
        <textarea
          value={context}
          onChange={(e) => setContext(e.target.value)}
          className="w-full border rounded-md px-2 py-1 text-sm h-32 font-mono"
          placeholder='{"input": {"value": "test"}}'
        />
      </div>

      <button
        onClick={handleExecute}
        disabled={isExecuting || !workflowId}
        className="w-full px-3 py-2 bg-indigo-600 text-white rounded-md text-sm hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {isExecuting ? '执行中...' : '执行测试'}
      </button>

      {error && (
        <div className="p-3 bg-red-50 border border-red-200 rounded-md">
          <div className="text-sm text-red-700 font-medium">执行失败</div>
          <div className="text-xs text-red-600 mt-1">{error}</div>
        </div>
      )}

      {result && (
        <div className="p-3 bg-green-50 border border-green-200 rounded-md">
          <div className="text-sm text-green-700 font-medium mb-1">
            执行成功 {result.success !== false ? '' : '(部分成功)'}
          </div>
          {result.output && (
            <div className="text-xs text-green-600 mt-1">
              <pre className="whitespace-pre-wrap">{JSON.stringify(result.output, null, 2)}</pre>
            </div>
          )}
          {result.error && (
            <div className="text-xs text-red-600 mt-1">
              错误: {result.error}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

const nodeTypes: NodeTypes = {
  start: StartNode,
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
  END: EndNode,
  JOIN: JoinNode,
};

// Node degree constraints: { maxInDegree, maxOutDegree }
// null means unlimited
const nodeDegreeConstraints: Record<string, { maxIn: number | null; maxOut: number | null }> = {
  START: { maxIn: 0, maxOut: null },           // 入度必须为0，出度无限制
  END: { maxIn: null, maxOut: 0 },             // 入度无限制，出度必须为0
  DATA_PROCESSING: { maxIn: 1, maxOut: 1 },    // 单输入单输出
  CONDITION: { maxIn: 1, maxOut: null },       // 单输入，多条件输出
  HTTP_REQUEST: { maxIn: 1, maxOut: 1 },      // 单输入单输出
  LLM_CALL: { maxIn: 1, maxOut: 1 },           // 单输入单输出
  PARALLEL: { maxIn: 1, maxOut: null },       // 单输入，多分支输出
  FOREACH: { maxIn: 1, maxOut: 1 },           // 单输入单输出
  BRANCH: { maxIn: 1, maxOut: null },          // 单输入，多条件输出
  SUBWORKFLOW: { maxIn: 1, maxOut: 1 },       // 单输入单输出
  TRY_CATCH: { maxIn: 1, maxOut: 1 },         // 单输入单输出
  RETRY: { maxIn: 1, maxOut: 1 },             // 单输入单输出
  JOIN: { maxIn: null, maxOut: 1 },            // 无限输入，单输出
};

const defaultNodes: Node[] = [
  {
    id: 'start',
    type: 'start',
    position: { x: 100, y: 200 },
    data: {
      label: '开始',
      type: 'START',
      config: { triggerType: 'NONE' },
      showDegree: true,
      inDegree: 0,
      outDegree: 0,
      maxIn: 0,
      maxOut: null,
    },
  },
];

const defaultEdges: Edge[] = [];

// Topological sort helper - sort nodes and compute positions for branch nodes
function topologicalSortAndPosition(
  nodeMap: Record<string, WorkflowNode>,
  edges: WorkflowEdge[],
  startNodeId: string
): Array<{ id: string; position: { x: number; y: number } }> {
  const nodeIds = Object.keys(nodeMap);
  if (nodeIds.length === 0) return [];

  // Build adjacency list and reverse adjacency list
  const adjList: Record<string, string[]> = {};
  const revAdjList: Record<string, string[]> = {};
  const inDegree: Record<string, number> = {};

  nodeIds.forEach(id => {
    adjList[id] = [];
    revAdjList[id] = [];
    inDegree[id] = 0;
  });

  // Build edges (from -> to)
  edges.forEach(edge => {
    if (adjList[edge.fromNodeId]) {
      adjList[edge.fromNodeId].push(edge.toNodeId);
      revAdjList[edge.toNodeId].push(edge.fromNodeId);
      inDegree[edge.toNodeId]++;
    }
  });

  // Find START node
  let startId = startNodeId;
  if (!startId || !nodeMap[startId]) {
    const potentialStarts = nodeIds.filter(id => inDegree[id] === 0);
    const startNode = potentialStarts.find(id => (nodeMap[id] as any)?.type === 'START');
    startId = startNode || potentialStarts[0] || nodeIds[0];
  }

  // Branch node types that need vertical layout
  const branchTypes = ['PARALLEL', 'FOREACH', 'BRANCH'];

  // Compute positions using BFS with vertical offset for branches
  const positions: Record<string, { x: number; y: number }> = {};
  const visited = new Set<string>();

  // Track y offset for each level
  let currentX = 100;
  const ySpacing = 100;
  const branchXOffset = 200; // horizontal offset for branch nodes

  // BFS from START
  const queue: Array<{ id: string; x: number; y: number }> = [{ id: startId, x: currentX, y: 200 }];

  while (queue.length > 0) {
    const { id, x, y } = queue.shift()!;
    if (visited.has(id)) continue;
    visited.add(id);

    const nodeType = (nodeMap[id] as any)?.type;
    positions[id] = { x, y };

    // Check if this is a branch node with multiple children
    const children = adjList[id] || [];
    const isBranchNode = branchTypes.includes(nodeType);

    if (isBranchNode && children.length > 1) {
      // Place branches vertically below, starting from current x
      const startY = y + ySpacing;
      children.forEach((childId, idx) => {
        const newY = startY + idx * ySpacing;
        if (!visited.has(childId)) {
          queue.push({ id: childId, x: x + branchXOffset, y: newY });
        }
      });
    } else {
      // Regular sequential flow - place horizontally
      children.forEach((childId) => {
        if (!visited.has(childId)) {
          queue.push({ id: childId, x: x + 250, y: 200 });
        }
      });
    }
  }

  // Handle any unvisited nodes
  nodeIds.forEach((id) => {
    if (!visited.has(id)) {
      positions[id] = { x: currentX, y: 200 + Math.random() * 100 };
      currentX += 250;
    }
  });

  return nodeIds.map(id => ({
    id,
    position: positions[id] || { x: 100, y: 200 }
  }));
}

// Helper function to check if workflow can enable trigger
function canEnableTrigger(workflow: { nodes?: Record<string, WorkflowNode>; startNodeId?: string }): boolean {
  if (!workflow.nodes || !workflow.startNodeId) return false;
  const startNode = workflow.nodes[workflow.startNodeId];
  if (!startNode || startNode.type !== 'START') return false;
  const config = startNode.config as any;
  return config?.triggerType && config.triggerType !== 'NONE';
}

// Helper to compute in/out degrees for all nodes
function computeNodeDegrees(
  nodes: Node[],
  edges: Edge[]
): Record<string, { inDegree: number; outDegree: number }> {
  const degrees: Record<string, { inDegree: number; outDegree: number }> = {};
  nodes.forEach(n => { degrees[n.id] = { inDegree: 0, outDegree: 0 }; });
  edges.forEach(e => {
    if (degrees[e.source]) degrees[e.source].outDegree++;
    if (degrees[e.target]) degrees[e.target].inDegree++;
  });
  return degrees;
}

// Helper to add degree info to node data
function addDegreeInfoToNodes(
  nodes: Node[],
  edges: Edge[],
  constraints: Record<string, { maxIn: number | null; maxOut: number | null }>
): Node[] {
  const degrees = computeNodeDegrees(nodes, edges);
  return nodes.map(n => {
    const nodeType = (n.data as any)?.type || n.type;
    const constraint = constraints[nodeType] || { maxIn: null, maxOut: null };
    const degree = degrees[n.id] || { inDegree: 0, outDegree: 0 };
    return {
      ...n,
      data: {
        ...n.data,
        showDegree: true,
        inDegree: degree.inDegree,
        outDegree: degree.outDegree,
        maxIn: constraint.maxIn,
        maxOut: constraint.maxOut,
      },
    };
  });
}

// Find branch nodes for a PARALLEL node based on edges
function findBranchesForParallel(
  nodes: Node[],
  edges: Edge[],
  parallelNodeId: string
): string[] {
  // Find all edges from PARALLEL to other nodes
  // Those target nodes are the branches
  return edges
    .filter(e => e.source === parallelNodeId)
    .map(e => e.target);
}

// Find JOIN node for a PARALLEL
function findJoinForParallel(
  nodes: Node[],
  edges: Edge[],
  parallelNodeId: string
): string | null {
  const branchNodeIds = findBranchesForParallel(nodes, edges, parallelNodeId);
  // JOIN is a node that:
  // 1. Has incoming edges from branch nodes
  // 2. Has outgoing edge to continuation
  const potentialJoinNodes = nodes.filter(n => {
    const incomingFromBranches = edges.filter(
      e => e.target === n.id && branchNodeIds.includes(e.source)
    );
    return incomingFromBranches.length >= 2;
  });

  if (potentialJoinNodes.length === 1) {
    return potentialJoinNodes[0].id;
  }
  return null;
}

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
  const [workflowEnabled, setWorkflowEnabled] = useState(false);
  const [selectedNode, setSelectedNode] = useState<Node | null>(null);
  const [selectedEdge, setSelectedEdge] = useState<Edge | null>(null);
  const [nodeConfig, setNodeConfig] = useState<Record<string, any>>({});
  const [saveWarning, setSaveWarning] = useState<string | null>(null);
  const [rightTab, setRightTab] = useState<'config' | 'execute'>('config');
  const [justSaved, setJustSaved] = useState(false);
  const [showTemplateModal, setShowTemplateModal] = useState(false);

  // Load existing workflow if editing
  useMemo(() => {
    if (existingWorkflow) {
      setWorkflowName(existingWorkflow.name);
      setWorkflowEnabled(existingWorkflow.enabled || false);

      // Topological sort with position computation for branch nodes
      const nodeMap = existingWorkflow.nodes || {};
      const edges = existingWorkflow.edges || [];
      const startNodeId = existingWorkflow.startNodeId;
      const positionedNodes = topologicalSortAndPosition(nodeMap, edges, startNodeId);

      const loadedNodes: Node[] = positionedNodes.map(({ id, position }) => {
        const node = nodeMap[id];
        // Map backend node type to ReactFlow node type
        const backendType = (node as any).type || 'DATA_PROCESSING';
        const rfType = backendType === 'START' ? 'start' : backendType;
        return {
          id,
          type: rfType,
          position,
          data: { ...(node as any), type: backendType, label: (node as any).name || id },
        };
      });
      // Add degree info to nodes
      const loadedEdges: Edge[] = edges.map((e: WorkflowEdge) => ({
        id: e.id,
        source: e.fromNodeId,
        target: e.toNodeId,
        animated: true,
      }));
      const nodesWithDegree = addDegreeInfoToNodes(loadedNodes, loadedEdges, nodeDegreeConstraints);
      setNodes(nodesWithDegree);
      setEdges(loadedEdges);
    }
  }, [existingWorkflow, setNodes, setEdges]);

  const onConnect = useCallback(
    (params: Connection) => {
      const sourceNode = nodes.find(n => n.id === params.source);
      const targetNode = nodes.find(n => n.id === params.target);
      if (!sourceNode || !targetNode) return;

      const sourceType = (sourceNode.data as any)?.type || sourceNode.type;
      const targetType = (targetNode.data as any)?.type || targetNode.type;

      // Compute current degrees
      const sourceOutDegree = edges.filter(e => e.source === params.source).length;
      const targetInDegree = edges.filter(e => e.target === params.target).length;

      // Check source node out-degree constraint
      const sourceConstraint = nodeDegreeConstraints[sourceType];
      if (sourceConstraint?.maxOut !== null && sourceOutDegree >= sourceConstraint.maxOut) {
        window.alert(`${sourceNode.data?.label || sourceType} 节点出度已达到上限(${sourceConstraint.maxOut})，无法再连接`);
        return;
      }

      // Check target node in-degree constraint
      const targetConstraint = nodeDegreeConstraints[targetType];
      if (targetConstraint?.maxIn !== null && targetInDegree >= targetConstraint.maxIn) {
        window.alert(`${targetNode.data?.label || targetType} 节点入度已达到上限(${targetConstraint.maxIn})，无法再连接`);
        return;
      }

      setEdges((eds) => {
        const newEdges = addEdge(
          {
            ...params,
            animated: true,
            id: `edge-${Date.now()}`,
          },
          eds
        );
        // Update nodes with new degree info
        const currentNodes = nodes;
        const nodesWithDegree = addDegreeInfoToNodes(currentNodes, newEdges, nodeDegreeConstraints);
        setNodes(nodesWithDegree);
        return newEdges;
      });
    },
    [nodes]
  );

  const onNodeClick = useCallback((_event: React.MouseEvent, node: Node) => {
    setSelectedNode(node);
    setSelectedEdge(null);
    setNodeConfig((node.data as any)?.config || {});
    setJustSaved(false);
  }, []);

  const onEdgeClick = useCallback((_event: React.MouseEvent, edge: Edge) => {
    setSelectedEdge(edge);
    setSelectedNode(null);
    setJustSaved(false);
  }, []);

  const deleteSelectedEdge = useCallback(() => {
    if (!selectedEdge) return;
    setEdges((eds) => {
      const newEdges = eds.filter((e) => e.id !== selectedEdge.id);
      // Update nodes with new degree info
      const nodesWithDegree = addDegreeInfoToNodes(nodes, newEdges, nodeDegreeConstraints);
      setNodes(nodesWithDegree);
      return newEdges;
    });
    setSelectedEdge(null);
  }, [selectedEdge, nodes]);

  const addNode = (type: string) => {
    const typeLabels: Record<string, string> = {
      start: '开始',
      END: '结束',
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
      JOIN: '新汇聚节点',
    };

    const defaultConfigs: Record<string, Record<string, unknown>> = {
      start: { triggerType: 'NONE' },
      END: {},
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
      JOIN: {},
    };

    const nodeType = type === 'start' ? 'START' : type;
    const constraint = nodeDegreeConstraints[nodeType] || { maxIn: null, maxOut: null };

    const newNode: Node = {
      id: `${type}-${Date.now()}`,
      type,
      position: { x: Math.random() * 400 + 100, y: Math.random() * 300 + 100 },
      data: {
        label: typeLabels[type] || '新节点',
        type: nodeType,
        config: defaultConfigs[type] || {},
        showDegree: true,
        inDegree: 0,
        outDegree: 0,
        maxIn: constraint.maxIn,
        maxOut: constraint.maxOut,
      },
    };
    setNodes((nds) => {
      const newNodes = [...nds, newNode];
      return addDegreeInfoToNodes(newNodes, edges, nodeDegreeConstraints);
    });
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
    setNodes((nds) => {
      const newNodes = nds.filter((n) => n.id !== selectedNode.id);
      const newEdges = edges.filter((e) => e.source !== selectedNode.id && e.target !== selectedNode.id);
      return addDegreeInfoToNodes(newNodes, newEdges, nodeDegreeConstraints);
    });
    setEdges((eds) => eds.filter((e) => e.source !== selectedNode.id && e.target !== selectedNode.id));
    setSelectedNode(null);
  };

  const saveWorkflow = async () => {
    // Validation
    const currentWorkflowName = workflowName;

    // 1. Check workflow name is not empty
    if (!currentWorkflowName.trim()) {
      setSaveWarning('工作流名称不能为空');
      return;
    }

    // 2. Check for duplicate workflow name
    const isDuplicate = workflows?.some(w =>
      w.name === currentWorkflowName && w.id !== workflowId
    );
    if (isDuplicate) {
      setSaveWarning(`工作流名称 "${currentWorkflowName}" 已存在`);
      return;
    }

    // 3. Check START node uniqueness
    const startNodes = nodes.filter(n => n.data?.type === 'START');
    if (startNodes.length === 0) {
      setSaveWarning('工作流至少需要一个开始节点');
      return;
    }
    if (startNodes.length > 1) {
      setSaveWarning('工作流只能有一个开始节点');
      return;
    }

    // 4. Check node name uniqueness
    const nodeNameCount: Record<string, number> = {};
    nodes.forEach(n => {
      const name = (n.data as any)?.label || n.id;
      nodeNameCount[name] = (nodeNameCount[name] || 0) + 1;
    });
    const duplicateName = Object.entries(nodeNameCount).find(([_, count]) => count > 1);
    if (duplicateName) {
      setSaveWarning(`节点名称 "${duplicateName[0]}" 已重复`);
      return;
    }

    // Auto-populate branchNodeIds for PARALLEL nodes
    nodes.forEach((node) => {
      const nodeType = (node.data as any)?.type;
      if (nodeType === 'PARALLEL') {
        const branches = findBranchesForParallel(nodes, edges, node.id);
        const joinNodeId = findJoinForParallel(nodes, edges, node.id);
        if (branches.length > 0) {
          node.data = {
            ...node.data,
            config: {
              ...(node.data as any)?.config,
              branchNodeIds: branches,
              nextNodeId: joinNodeId,  // Auto-set JOIN as next
            },
          };
        }
      }
    });

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

    const startNodeId = nodes.find(n => n.data?.type === 'START')?.id || '';

    const workflow: Workflow = {
      id: workflowId || `wf-${Date.now()}`,
      name: currentWorkflowName,
      startNodeId,
      nodes: workflowNodes,
      edges: workflowEdges,
      enabled: workflowEnabled,
    };

    try {
      await createWorkflow.mutateAsync(workflow);
      setJustSaved(true);
      setSelectedNode(null);
      navigate('/');
    } catch (error: any) {
      console.error('Save failed:', error);
      const errorMessage = error?.response?.data?.errors?.[0]?.message
        || error?.message
        || '保存失败';
      setSaveWarning(errorMessage);
    }
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
          <div className="flex items-center gap-4">
            <label className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={workflowEnabled}
                onChange={(e) => setWorkflowEnabled(e.target.checked)}
                disabled={!canEnableTrigger({ nodes: nodes.reduce((acc, n) => ({ ...acc, [n.id]: { type: n.data?.type, config: n.data?.config } }), {}), startNodeId: nodes[0]?.id })}
                className="w-4 h-4"
              />
              <span className="text-sm font-medium">启用定时/Webhook</span>
            </label>
          </div>
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
            onEdgeClick={onEdgeClick}
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
              onClick={() => setRightTab('execute')}
              className={`flex-1 px-4 py-2 text-sm font-medium ${
                rightTab === 'execute'
                  ? 'text-indigo-600 border-b-2 border-indigo-600'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              执行测试
            </button>
          </div>

          {/* Tab Content */}
          <div className="flex-1 overflow-y-auto p-4">
            {rightTab === 'execute' ? (
              <ExecutePanel selectedNode={selectedNode} workflowId={workflowId || ''} />
            ) : selectedEdge ? (
              /* Edge Config Panel when edge selected */
              <div className="space-y-3">
                <h3 className="font-bold text-gray-900">关系配置</h3>
                <div className="p-3 bg-gray-50 rounded-md space-y-2">
                  <div className="text-sm">
                    <span className="text-gray-500">从节点: </span>
                    <span className="font-medium text-gray-900">
                      {String(nodes.find(n => n.id === selectedEdge.source)?.data?.label || selectedEdge.source)}
                    </span>
                  </div>
                  <div className="text-sm">
                    <span className="text-gray-500">到节点: </span>
                    <span className="font-medium text-gray-900">
                      {String(nodes.find(n => n.id === selectedEdge.target)?.data?.label || selectedEdge.target)}
                    </span>
                  </div>
                </div>
                <div className="flex space-x-2 pt-2">
                  <button
                    onClick={deleteSelectedEdge}
                    className="flex-1 px-3 py-1.5 bg-red-100 text-red-700 rounded-md text-sm hover:bg-red-200"
                  >
                    删除连线
                  </button>
                </div>
              </div>
            ) : selectedNode ? (
              /* Node Config Panel when node selected */
              <div className="space-y-3">
                <h3 className="font-bold text-gray-900">节点配置</h3>

                {/* Load Config Button */}
                <button
                  onClick={() => setShowTemplateModal(true)}
                  className="w-full px-3 py-2 bg-indigo-50 text-indigo-700 rounded-md text-sm hover:bg-indigo-100 border border-indigo-200"
                >
                  加载配置模板
                </button>
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
                {(selectedNode.type === 'start' || selectedNode.data?.type === 'START') && (
                  <>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">触发类型</label>
                      <select
                        value={nodeConfig.triggerType || 'NONE'}
                        onChange={(e) => {
                          const newType = e.target.value as 'NONE' | 'SCHEDULE' | 'WEBHOOK';
                          setNodeConfig({
                            ...nodeConfig,
                            triggerType: newType,
                            cronExpression: newType === 'SCHEDULE' ? nodeConfig.cronExpression || '0 * * * *' : nodeConfig.cronExpression,
                            webhookPath: newType === 'WEBHOOK' ? nodeConfig.webhookPath || '/webhook' : nodeConfig.webhookPath,
                            fieldMappings: newType === 'WEBHOOK' ? nodeConfig.fieldMappings || [] : nodeConfig.fieldMappings,
                          });
                        }}
                        className="w-full border rounded-md px-2 py-1 text-sm"
                      >
                        <option value="NONE">无 (None)</option>
                        <option value="SCHEDULE">定时调度 (Schedule)</option>
                        <option value="WEBHOOK">Webhook</option>
                      </select>
                    </div>
                    {nodeConfig.triggerType === 'SCHEDULE' && (
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Cron 表达式</label>
                        <input
                          type="text"
                          value={nodeConfig.cronExpression || '0 * * * *'}
                          onChange={(e) => setNodeConfig({ ...nodeConfig, cronExpression: e.target.value })}
                          className="w-full border rounded-md px-2 py-1 text-sm"
                          placeholder="0 * * * *"
                        />
                        <div className="text-xs text-gray-500 mt-1">
                          格式: 分 时 日 月 周 (例如: "0 * * * *" 表示每小时)
                        </div>
                      </div>
                    )}
                    {nodeConfig.triggerType === 'WEBHOOK' && (
                      <>
                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-1">Webhook 路径</label>
                          <input
                            type="text"
                            value={nodeConfig.webhookPath || '/webhook'}
                            onChange={(e) => setNodeConfig({ ...nodeConfig, webhookPath: e.target.value })}
                            className="w-full border rounded-md px-2 py-1 text-sm"
                            placeholder="/webhook/order"
                          />
                        </div>
                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-1">字段映射 (JSON)</label>
                          <textarea
                            value={JSON.stringify(nodeConfig.fieldMappings || [], null, 2)}
                            onChange={(e) => {
                              try {
                                setNodeConfig({ ...nodeConfig, fieldMappings: JSON.parse(e.target.value) });
                              } catch {}
                            }}
                            className="w-full border rounded-md px-2 py-1 text-sm h-20"
                            placeholder='[{"sourceField": "orderId", "targetPath": "order.id"}]'
                          />
                        </div>
                      </>
                    )}
                    <div className="mt-4 pt-4 border-t">
                      <button
                        onClick={async () => {
                          if (!workflowId) {
                            window.alert('工作流 ID 不可用，请先保存工作流');
                            return;
                          }
                          try {
                            const response = await fetch(`/api/workflows/${workflowId}/execute`, {
                              method: 'POST',
                              headers: { 'Content-Type': 'application/json' },
                              body: JSON.stringify({ context: {} }),
                            });
                            if (!response.ok) throw new Error(`执行失败: ${response.statusText}`);
                            window.alert('触发成功！工作流已开始执行');
                          } catch (error) {
                            window.alert(`触发失败: ${error instanceof Error ? error.message : '未知错误'}`);
                          }
                        }}
                        disabled={!workflowEnabled || !nodeConfig.triggerType || nodeConfig.triggerType === 'NONE'}
                        className="w-full px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed"
                      >
                        手动触发
                      </button>
                      {!workflowEnabled && <p className="text-xs text-gray-500 mt-1">启用工作流后才能触发</p>}
                      {workflowEnabled && nodeConfig.triggerType === 'NONE' && <p className="text-xs text-gray-500 mt-1">请选择触发类型</p>}
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

      {/* Template Selection Modal */}
      {showTemplateModal && selectedNode && (
        <TemplateSelectModal
          nodeType={selectedNode.type || ''}
          onSelect={(template) => {
            setNodeConfig({ ...nodeConfig, ...template.config });
            setShowTemplateModal(false);
          }}
          onClose={() => setShowTemplateModal(false)}
        />
      )}
    </div>
  );
}
