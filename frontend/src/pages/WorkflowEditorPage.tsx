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
  Panel,
  NodeTypes,
  Handle,
  Position,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { useCreateWorkflow, useWorkflow } from '../hooks/useWorkflow';
import { useNavigate, useSearchParams } from 'react-router-dom';
import type { Workflow, WorkflowNode, WorkflowEdge } from '../types/workflow';

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
  const createWorkflow = useCreateWorkflow();

  const [nodes, setNodes, onNodesChange] = useNodesState(defaultNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(defaultEdges);
  const [workflowName, setWorkflowName] = useState('新工作流');
  const [selectedNode, setSelectedNode] = useState<Node | null>(null);
  const [nodeConfig, setNodeConfig] = useState<Record<string, any>>({});

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
            onChange={(e) => setWorkflowName(e.target.value)}
            className="text-xl font-bold border-b-2 border-indigo-500 bg-transparent px-2 py-1 focus:outline-none"
            placeholder="工作流名称"
          />
        </div>
        <div className="flex items-center space-x-2">
          <button
            onClick={() => addNode('DATA_PROCESSING')}
            className="px-3 py-1.5 bg-indigo-100 text-indigo-700 rounded-md text-sm hover:bg-indigo-200"
          >
            + 数据节点
          </button>
          <button
            onClick={() => addNode('CONDITION')}
            className="px-3 py-1.5 bg-amber-100 text-amber-700 rounded-md text-sm hover:bg-amber-200"
          >
            + 条件节点
          </button>
          <button
            onClick={() => addNode('HTTP_REQUEST')}
            className="px-3 py-1.5 bg-green-100 text-green-700 rounded-md text-sm hover:bg-green-200"
          >
            + HTTP
          </button>
          <button
            onClick={() => addNode('LLM_CALL')}
            className="px-3 py-1.5 bg-purple-100 text-purple-700 rounded-md text-sm hover:bg-purple-200"
          >
            + LLM
          </button>
          <button
            onClick={() => addNode('PARALLEL')}
            className="px-3 py-1.5 bg-cyan-100 text-cyan-700 rounded-md text-sm hover:bg-cyan-200"
          >
            + 并行
          </button>
          <button
            onClick={() => addNode('FOREACH')}
            className="px-3 py-1.5 bg-pink-100 text-pink-700 rounded-md text-sm hover:bg-pink-200"
          >
            + 循环
          </button>
          <button
            onClick={() => addNode('BRANCH')}
            className="px-3 py-1.5 bg-orange-100 text-orange-700 rounded-md text-sm hover:bg-orange-200"
          >
            + 分支
          </button>
          <button
            onClick={() => addNode('SUBWORKFLOW')}
            className="px-3 py-1.5 bg-teal-100 text-teal-700 rounded-md text-sm hover:bg-teal-200"
          >
            + 子工作流
          </button>
          <button
            onClick={() => addNode('TRY_CATCH')}
            className="px-3 py-1.5 bg-red-100 text-red-700 rounded-md text-sm hover:bg-red-200"
          >
            + 异常
          </button>
          <button
            onClick={() => addNode('RETRY')}
            className="px-3 py-1.5 bg-yellow-100 text-yellow-700 rounded-md text-sm hover:bg-yellow-200"
          >
            + 重试
          </button>
          <button
            onClick={saveWorkflow}
            className="px-4 py-1.5 bg-indigo-600 text-white rounded-md text-sm hover:bg-indigo-700"
          >
            保存
          </button>
        </div>
      </div>

      {/* Canvas */}
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
          <Panel position="top-left" className="bg-white p-3 rounded-lg shadow-md">
            <div className="text-sm font-medium text-gray-700 mb-2">拖拽节点到画布上</div>
            <div className="text-xs text-gray-500">
              点击节点可配置属性，通过拖拽连接节点
            </div>
          </Panel>
        </ReactFlow>

        {/* Node Config Panel */}
        {selectedNode && (
          <div className="absolute right-4 top-4 w-80 bg-white rounded-lg shadow-lg p-4 border">
            <h3 className="font-bold text-gray-900 mb-3">节点配置</h3>
            <div className="space-y-3">
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
          </div>
        )}
      </div>
    </div>
  );
}
