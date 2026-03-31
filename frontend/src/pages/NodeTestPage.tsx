import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useWorkflow, useTestNode } from '../hooks/useWorkflow';

export function NodeTestPage() {
  const { workflowId, nodeId } = useParams<{ workflowId: string; nodeId: string }>();
  const navigate = useNavigate();
  const { data: workflow, isLoading } = useWorkflow(workflowId || '');
  const testNode = useTestNode();

  const [inputContext, setInputContext] = useState('{"data": {"value": 100}}');
  const [result, setResult] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  if (isLoading) {
    return <div className="text-center py-10">加载中...</div>;
  }

  if (!workflow || !nodeId) {
    return (
      <div className="text-center py-10">
        <p className="text-gray-500 mb-4">工作流或节点不存在</p>
        <button
          onClick={() => navigate('/')}
          className="text-indigo-600 hover:text-indigo-800"
        >
          返回首页
        </button>
      </div>
    );
  }

  const node = workflow.nodes?.[nodeId];

  if (!node) {
    return (
      <div className="text-center py-10">
        <p className="text-gray-500 mb-4">节点不存在: {nodeId}</p>
        <button
          onClick={() => navigate('/')}
          className="text-indigo-600 hover:text-indigo-800"
        >
          返回首页
        </button>
      </div>
    );
  }

  const handleTest = async () => {
    setError(null);
    setResult(null);
    try {
      const context = JSON.parse(inputContext);
      const res = await testNode.mutateAsync({
        workflowId: workflowId!,
        nodeId: nodeId!,
        context,
      });
      setResult(res);
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || '测试失败');
    }
  };

  return (
    <div className="max-w-4xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <button
            onClick={() => navigate('/')}
            className="text-sm text-gray-500 hover:text-gray-700 mb-1"
          >
            ← 返回
          </button>
          <h1 className="text-2xl font-bold text-gray-900">节点测试</h1>
          <p className="text-gray-500 text-sm mt-1">
            工作流: {workflow.name} | 节点: {node.name || nodeId}
          </p>
        </div>
        <span
          className={`px-3 py-1 rounded-full text-sm font-medium ${
            node.type === 'DATA_PROCESSING'
              ? 'bg-indigo-100 text-indigo-800'
              : 'bg-amber-100 text-amber-800'
          }`}
        >
          {node.type === 'DATA_PROCESSING' ? '数据处理' : '条件分支'}
        </span>
      </div>

      <div className="grid grid-cols-2 gap-6">
        {/* Node Config */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">节点配置</h2>
          <div className="space-y-3 text-sm">
            <div>
              <span className="font-medium text-gray-700">节点 ID:</span>
              <span className="ml-2 text-gray-900">{nodeId}</span>
            </div>
            <div>
              <span className="font-medium text-gray-700">名称:</span>
              <span className="ml-2 text-gray-900">{node.name || '-'}</span>
            </div>
            <div>
              <span className="font-medium text-gray-700 block mb-2">配置:</span>
              <pre className="bg-gray-50 p-3 rounded-md text-xs overflow-auto">
                {JSON.stringify(node.config || {}, null, 2)}
              </pre>
            </div>
            {node.inputMapping && Object.keys(node.inputMapping).length > 0 && (
              <div>
                <span className="font-medium text-gray-700 block mb-2">输入映射:</span>
                <pre className="bg-gray-50 p-3 rounded-md text-xs overflow-auto">
                  {JSON.stringify(node.inputMapping, null, 2)}
                </pre>
              </div>
            )}
            {node.outputMapping && Object.keys(node.outputMapping).length > 0 && (
              <div>
                <span className="font-medium text-gray-700 block mb-2">输出映射:</span>
                <pre className="bg-gray-50 p-3 rounded-md text-xs overflow-auto">
                  {JSON.stringify(node.outputMapping, null, 2)}
                </pre>
              </div>
            )}
          </div>
        </div>

        {/* Test Area */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">测试执行</h2>

          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              输入 Context (JSON)
            </label>
            <textarea
              value={inputContext}
              onChange={(e) => setInputContext(e.target.value)}
              className="w-full border border-gray-300 rounded-md px-3 py-2 h-32 text-sm font-mono focus:ring-indigo-500 focus:border-indigo-500"
              placeholder='{"data": {"value": 100}}'
            />
            <p className="text-xs text-gray-500 mt-1">
              使用 #input.x 引用输入变量，如 #input.value
            </p>
          </div>

          <button
            onClick={handleTest}
            disabled={testNode.isPending}
            className="w-full bg-indigo-600 text-white py-2 px-4 rounded-md hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {testNode.isPending ? '执行中...' : '执行测试'}
          </button>

          {error && (
            <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-md">
              <p className="text-red-700 text-sm">{error}</p>
            </div>
          )}

          {result && (
            <div className="mt-4">
              <div className="flex items-center justify-between mb-2">
                <span className="font-medium text-gray-700">执行结果</span>
                <span
                  className={`px-2 py-0.5 rounded text-xs ${
                    result.status === 'SUCCESS'
                      ? 'bg-green-100 text-green-800'
                      : 'bg-red-100 text-red-800'
                  }`}
                >
                  {result.status === 'SUCCESS' ? '成功' : '失败'}
                </span>
              </div>
              <pre className="bg-gray-50 p-3 rounded-md text-xs overflow-auto">
                {JSON.stringify(result, null, 2)}
              </pre>
            </div>
          )}
        </div>
      </div>

      {/* Expression Help */}
      <div className="mt-6 bg-gray-50 rounded-lg p-4">
        <h3 className="font-medium text-gray-900 mb-2">表达式语法说明</h3>
        <div className="grid grid-cols-2 gap-4 text-sm">
          <div>
            <p className="text-gray-700 font-medium mb-1">数据处理节点</p>
            <ul className="text-gray-600 space-y-1">
              <li>• 表达式示例: <code className="bg-white px-1 rounded">#input.amount * 0.9</code></li>
              <li>• 输出 Key: 指定结果存储的键名</li>
              <li>• 支持算术运算: +, -, *, /</li>
            </ul>
          </div>
          <div>
            <p className="text-gray-700 font-medium mb-1">条件分支节点</p>
            <ul className="text-gray-600 space-y-1">
              <li>• 条件示例: <code className="bg-white px-1 rounded">#input.amount {'>'} 1000</code></li>
              <li>• 每个条件有表达式和下一节点 ID</li>
              <li>• 按顺序匹配，返回首个满足条件的下一节点</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
