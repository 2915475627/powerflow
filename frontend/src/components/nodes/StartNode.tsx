import { memo } from 'react';
import { Handle, Position } from '@xyflow/react';

const StartNode = ({ data }: { data: any }) => {
  return (
    <div className="px-4 py-2 bg-white border-2 border-green-500 rounded-lg shadow-md min-w-[150px]">
      <Handle type="target" position={Position.Left} className="w-3 h-3 bg-green-500" />
      <div className="font-medium text-gray-900">开始</div>
      <div className="text-xs text-gray-500">{data.label || '开始节点'}</div>
      <Handle type="source" position={Position.Right} className="w-3 h-3 bg-green-500" />
    </div>
  );
};

export default memo(StartNode);
