import { memo } from 'react';
import { Handle, Position, NodeProps } from '@xyflow/react';

interface ConditionNodeData {
  label?: string;
  conditions?: unknown[];
}

export const ConditionNode = memo(({ data, selected }: NodeProps) => {
  const nodeData = data as ConditionNodeData;

  return (
    <div className={`px-4 py-3 rounded-lg border-2 min-w-[150px] ${
      selected ? 'border-indigo-500 bg-indigo-50' : 'border-orange-300 bg-orange-50'
    }`}>
      <div className="flex items-center gap-2">
        <div className="w-3 h-3 rounded-full bg-orange-500" />
        <span className="font-medium text-gray-900">{nodeData.label || 'Condition'}</span>
      </div>
      <div className="mt-2 text-xs text-gray-500">
        {nodeData.conditions?.length || 0} conditions
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