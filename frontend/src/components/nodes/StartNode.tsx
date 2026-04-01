import { memo } from 'react';
import { Handle, Position } from '@xyflow/react';

const StartNode = () => {
  return (
    <div className="px-4 py-2 bg-green-500 text-white rounded-lg shadow-md min-w-[100px] text-center">
      <Handle type="source" position={Position.Right} />
      开始
    </div>
  );
};

export default memo(StartNode);
