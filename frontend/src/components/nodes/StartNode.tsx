import { memo } from 'react';
import { Handle, Position } from '@xyflow/react';

function DegreeBadge({ current, max, type }: { current: number; max: number | null; type: 'in' | 'out' }) {
  const maxDisplay = max === null ? '∞' : max;
  const isLimited = max !== null;
  const isFull = isLimited && current >= max;
  const colorClass = isFull ? 'bg-red-100 text-red-700' : 'bg-gray-100 text-gray-600';
  return (
    <span className={`text-xs px-1 py-0.5 rounded ${colorClass}`} title={type === 'in' ? '入度' : '出度'}>
      {type === 'in' ? '入' : '出'}:{current}/{maxDisplay}
    </span>
  );
}

const StartNode = ({ data }: { data: any }) => {
  return (
    <div className="px-4 py-2 bg-white border-2 border-green-500 rounded-lg shadow-md min-w-[150px]">
      <Handle type="target" position={Position.Left} className="w-3 h-3 bg-green-500" />
      <div className="flex items-center justify-between">
        <div className="font-medium text-gray-900">开始</div>
        {data.showDegree && (
          <div className="flex gap-1">
            <DegreeBadge current={data.inDegree || 0} max={data.maxIn} type="in" />
            <DegreeBadge current={data.outDegree || 0} max={data.maxOut} type="out" />
          </div>
        )}
      </div>
      <div className="text-xs text-gray-500">{data.label || '开始节点'}</div>
      <Handle type="source" position={Position.Right} className="w-3 h-3 bg-green-500" />
    </div>
  );
};

export default memo(StartNode);
