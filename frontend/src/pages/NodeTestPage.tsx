import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useWorkflow, useTestNode } from '../hooks/useWorkflow';
import type { Context, NodeResult } from '../types/workflow';

export function NodeTestPage() {
  const { workflowId, nodeId } = useParams<{ workflowId: string; nodeId: string }>();
  const { data: workflow, isLoading } = useWorkflow(workflowId || '');
  const testNode = useTestNode();
  const [inputJson, setInputJson] = useState('{"amount": 1000}');
  const [result, setResult] = useState<NodeResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const node = workflow?.nodes?.find((n: any) => n.id === nodeId);

  const handleTest = async () => {
    if (!workflowId || !nodeId) return;
    try {
      setError(null);
      const context: Context = { data: JSON.parse(inputJson) };
      const res = await testNode.mutateAsync({ workflowId, nodeId, context });
      setResult(res);
    } catch (err) {
      setError(err instanceof Error ? err.message : '测试失败');
    }
  };

  if (isLoading) return <div className="text-center py-10">加载中...</div>;
  if (!workflow || !node) return <div className="text-center py-10">工作流或节点不存在</div>;

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">测试节点</h1>
        <p className="text-gray-600">
          工作流: {workflow.name} / 节点: {node.name} ({node.type})
        </p>
      </div>

      <div className="grid grid-cols-2 gap-6">
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-semibold mb-4">节点配置</h2>
          <pre className="bg-gray-50 p-4 rounded-md overflow-auto">
            {JSON.stringify(node.config, null, 2)}
          </pre>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-semibold mb-4">输入映射</h2>
          <pre className="bg-gray-50 p-4 rounded-md overflow-auto">
            {JSON.stringify(node.inputMapping, null, 2)}
          </pre>
        </div>
      </div>

      <div className="mt-6 bg-white rounded-lg shadow p-6">
        <h2 className="text-lg font-semibold mb-4">执行测试</h2>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              输入上下文 (JSON)
            </label>
            <textarea
              value={inputJson}
              onChange={(e) => setInputJson(e.target.value)}
              className="w-full h-32 font-mono text-sm rounded-md border border-gray-300 px-3 py-2"
              placeholder='{"key": "value"}'
            />
          </div>
          <button
            onClick={handleTest}
            disabled={testNode.isPending}
            className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 disabled:bg-indigo-300"
          >
            {testNode.isPending ? '执行中...' : '执行节点'}
          </button>
        </div>

        {error && <div className="mt-4 p-4 bg-red-50 text-red-700 rounded-md">{error}</div>}

        {result && (
          <div className="mt-4">
            <h3 className="text-sm font-medium text-gray-700 mb-2">执行结果</h3>
            <pre className="bg-gray-50 p-4 rounded-md overflow-auto">
              {JSON.stringify(result, null, 2)}
            </pre>
          </div>
        )}
      </div>
    </div>
  );
}