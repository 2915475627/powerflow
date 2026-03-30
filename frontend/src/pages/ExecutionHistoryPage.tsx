import { useState } from 'react';
import { useWorkflows } from '../hooks/useWorkflow';
import { WorkflowExecutionTimeline } from '../components/WorkflowExecutionTimeline';

export function ExecutionHistoryPage() {
  const { data: workflows } = useWorkflows();
  const [selectedWorkflowId, setSelectedWorkflowId] = useState<string>('');

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">执行监控</h1>

      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          选择工作流
        </label>
        <select
          value={selectedWorkflowId}
          onChange={(e) => setSelectedWorkflowId(e.target.value)}
          className="w-full rounded-md border border-gray-300 px-3 py-2"
        >
          <option value="">请选择工作流</option>
          {workflows?.map((wf) => (
            <option key={wf.id} value={wf.id}>
              {wf.name}
            </option>
          ))}
        </select>
      </div>

      {selectedWorkflowId && (
        <WorkflowExecutionTimeline workflowId={selectedWorkflowId} />
      )}

      {!selectedWorkflowId && (
        <div className="text-center py-10 text-gray-500">
          请选择一个工作流查看执行历史
        </div>
      )}
    </div>
  );
}