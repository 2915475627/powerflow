import { useExecutionHistory } from '../hooks/useWorkflow';
import { NodeExecutionCard } from './NodeExecutionCard';
import type { WorkflowExecutionResult } from '../types/workflow';

interface Props {
  workflowId: string;
}

export function WorkflowExecutionTimeline({ workflowId }: Props) {
  const { data: executions, isLoading, error } = useExecutionHistory(workflowId);

  if (isLoading) return <div className="text-center py-10">加载中...</div>;
  if (error) return <div className="text-red-500 py-10">加载失败</div>;

  return (
    <div className="space-y-6">
      {executions?.map((execution) => (
        <ExecutionTimelineCard key={execution.executionId} execution={execution} />
      ))}

      {(!executions || executions.length === 0) && (
        <div className="text-center py-10 text-gray-500">暂无执行记录</div>
      )}
    </div>
  );
}

function ExecutionTimelineCard({ execution }: { execution: WorkflowExecutionResult }) {
  const totalDuration = execution.nodeExecutions.reduce((sum, ne) => sum + ne.durationMs, 0);

  return (
    <div className="bg-white rounded-lg shadow overflow-hidden">
      <div className="px-6 py-4 border-b flex justify-between items-center">
        <div>
          <span className="font-medium text-gray-900">执行ID: {execution.executionId}</span>
          <span
            className={`ml-4 px-2 py-1 rounded text-xs ${
              execution.status === 'SUCCESS'
                ? 'bg-green-100 text-green-800'
                : 'bg-red-100 text-red-800'
            }`}
          >
            {execution.status === 'SUCCESS' ? '成功' : '失败'}
          </span>
        </div>
        <div className="text-sm text-gray-500">
          总耗时: {totalDuration}ms | 节点数: {execution.nodeExecutions.length}
        </div>
      </div>

      {execution.error && (
        <div className="px-6 py-3 bg-red-50 text-red-700 text-sm">
          错误: {execution.error}
        </div>
      )}

      <div className="px-6 py-4">
        <h4 className="text-sm font-medium text-gray-700 mb-3">节点执行详情</h4>
        <div className="space-y-3">
          {execution.nodeExecutions.map((ne, index) => (
            <NodeExecutionCard key={ne.id} nodeExecution={ne} index={index + 1} />
          ))}
        </div>
      </div>
    </div>
  );
}