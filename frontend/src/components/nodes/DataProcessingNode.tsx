import { memo } from 'react';
import { Handle, Position, NodeProps } from '@xyflow/react';

interface DataProcessingNodeData {
  label?: string;
  config?: {
    expression?: string | (() => string);
    outputKey?: string;
  };
}

export const DataProcessingNode = memo(({ data, selected }: NodeProps) => {
  const nodeData = data as DataProcessingNodeData;
  const expression = nodeData.config?.expression?.toString() || 'No expression';

  return (
    <div className={`px-4 py-3 rounded-lg border-2 min-w-[150px] ${
      selected ? 'border-indigo-500 bg-indigo-50' : 'border-gray-300 bg-white'
    }`}>
      <div className="flex items-center gap-2">
        <div className="w-3 h-3 rounded-full bg-green-500" />
        <span className="font-medium text-gray-900">{nodeData.label || 'Data Processing'}</span>
      </div>
      <div className="mt-2 text-xs text-gray-500">
        {expression.substring(0, 30)}
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