import { useCallback } from 'react';
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

  const onConnect = useCallback(
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