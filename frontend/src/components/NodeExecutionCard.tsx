import { useState } from 'react';
import type { NodeExecution } from '../types/workflow';

interface Props {
  nodeExecution: NodeExecution;
  index: number;
}

export function NodeExecutionCard({ nodeExecution, index }: Props) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className="flex items-start gap-4 p-3 bg-gray-50 rounded-lg">
      <div className="flex-shrink-0 w-8 h-8 rounded-full bg-indigo-100 flex items-center justify-center text-sm font-medium text-indigo-700">
        {index}
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex justify-between items-start">
          <button
            onClick={() => setExpanded(!expanded)}
            className="font-medium text-gray-900 hover:text-indigo-600 cursor-pointer text-left"
          >
            {expanded ? '▼' : '▶'} 节点: {nodeExecution.nodeId}
          </button>
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

        {expanded && (
          <div className="mt-4 space-y-3">
            <div>
              <h5 className="text-sm font-medium text-gray-700 mb-1">输入 (input)</h5>
              <pre className="bg-gray-100 p-2 rounded text-xs overflow-auto max-h-40">
                {JSON.stringify(nodeExecution.input || {}, null, 2)}
              </pre>
            </div>
            <div>
              <h5 className="text-sm font-medium text-gray-700 mb-1">输出 (output)</h5>
              <pre className="bg-gray-100 p-2 rounded text-xs overflow-auto max-h-40">
                {JSON.stringify(nodeExecution.output || {}, null, 2)}
              </pre>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
