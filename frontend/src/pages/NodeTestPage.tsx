import { useParams } from 'react-router-dom';

export function NodeTestPage() {
  const { workflowId, nodeId } = useParams<{ workflowId: string; nodeId: string }>();

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">节点测试</h1>
      <div className="bg-white p-6 rounded-lg shadow">
        <p className="text-gray-600">
          工作流 ID: <span className="font-mono">{workflowId}</span>
        </p>
        <p className="text-gray-600 mt-2">
          节点 ID: <span className="font-mono">{nodeId}</span>
        </p>
      </div>
    </div>
  );
}