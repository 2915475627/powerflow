import type { NodeExecution } from '../types/workflow';

interface Props {
  nodeExecution: NodeExecution;
  index: number;
}

export function NodeExecutionCard({ nodeExecution, index }: Props) {
  return (
    <div className="flex items-start gap-4 p-3 bg-gray-50 rounded-lg">
      <div className="flex-shrink-0 w-8 h-8 rounded-full bg-indigo-100 flex items-center justify-center text-sm font-medium text-indigo-700">
        {index}
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex justify-between items-start">
          <span className="font-medium text-gray-900">节点: {nodeExecution.nodeId}</span>
          <span className="text-sm text-gray-500">{nodeExecution.durationMs}ms</span>
        </div>
        <div className="mt-1 text-sm text-gray-500">
          状态:{' '}
          <span
            className={`${
              nodeExecution.status === 'SUCCESS' ? 'text-green-600' : 'text-red-600'
            }`}
          >
            {nodeExecution.status === 'SUCCESS' ? '成功' : '失败'}
          </span>
        </div>
        {nodeExecution.error && (
          <div className="mt-2 text-sm text-red-600">{nodeExecution.error}</div>
        )}
        <div className="mt-2 text-xs text-gray-400">
          开始: {new Date(nodeExecution.startTime).toLocaleString()}
        </div>
      </div>
    </div>
  );
}