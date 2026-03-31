import { useState } from 'react';
import { useWorkflows, useCreateWorkflow, useDeleteWorkflow, useExecuteWorkflow } from '../hooks/useWorkflow';
import type { Workflow, Context } from '../types/workflow';

export function WorkflowsPage() {
  const { data: workflows, isLoading, error } = useWorkflows();
  const createWorkflow = useCreateWorkflow();
  const deleteWorkflow = useDeleteWorkflow();
  const executeWorkflow = useExecuteWorkflow();

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [executeContext, setExecuteContext] = useState('');
  const [executeResult, setExecuteResult] = useState<any>(null);
  const [selectedWorkflowId, setSelectedWorkflowId] = useState<string | null>(null);

  const handleCreate = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const workflow: Workflow = {
      id: formData.get('id') as string,
      name: formData.get('name') as string,
      description: formData.get('description') as string,
      startNodeId: formData.get('startNodeId') as string,
      nodes: {},
      edges: [],
    };
    await createWorkflow.mutateAsync(workflow);
    setShowCreateModal(false);
  };

  const handleExecute = async (workflowId: string) => {
    try {
      const context: Context = JSON.parse(executeContext || '{"data":{}}');
      const result = await executeWorkflow.mutateAsync({ workflowId, context });
      setExecuteResult(result);
    } catch (err: any) {
      setExecuteResult({ error: err.message });
    }
  };

  if (isLoading) return <div className="text-center py-10">加载中...</div>;
  if (error) return <div className="text-red-500 py-10">加载失败: {error.message}</div>;

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-900">工作流</h1>
        <button
          onClick={() => setShowCreateModal(true)}
          className="bg-indigo-600 text-white px-4 py-2 rounded-md hover:bg-indigo-700"
        >
          创建工作流
        </button>
      </div>

      {workflows && workflows.length > 0 ? (
        <div className="bg-white shadow rounded-lg overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">ID</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">名称</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">开始节点</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">节点数</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {workflows.map((workflow) => (
                <tr key={workflow.id}>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{workflow.id}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{workflow.name}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{workflow.startNodeId}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {Object.keys(workflow.nodes || {}).length}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 space-x-2">
                    <button
                      onClick={() => {
                        setSelectedWorkflowId(workflow.id);
                        setExecuteResult(null);
                      }}
                      className="text-indigo-600 hover:text-indigo-900"
                    >
                      执行
                    </button>
                    <button
                      onClick={() => deleteWorkflow.mutate(workflow.id)}
                      className="text-red-600 hover:text-red-900"
                    >
                      删除
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="text-center py-10 text-gray-500">
          暂无工作流，点击"创建工作流"开始
        </div>
      )}

      {/* Create Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <h2 className="text-xl font-bold mb-4">创建工作流</h2>
            <form onSubmit={handleCreate}>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-1">ID</label>
                <input
                  type="text"
                  name="id"
                  required
                  className="w-full rounded-md border border-gray-300 px-3 py-2"
                  placeholder="my-workflow"
                />
              </div>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-1">名称</label>
                <input
                  type="text"
                  name="name"
                  required
                  className="w-full rounded-md border border-gray-300 px-3 py-2"
                  placeholder="我的工作流"
                />
              </div>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-1">描述</label>
                <input
                  type="text"
                  name="description"
                  className="w-full rounded-md border border-gray-300 px-3 py-2"
                />
              </div>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-1">开始节点ID</label>
                <input
                  type="text"
                  name="startNodeId"
                  required
                  className="w-full rounded-md border border-gray-300 px-3 py-2"
                  placeholder="start"
                />
              </div>
              <div className="flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md"
                >
                  取消
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
                >
                  创建
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Execute Modal */}
      {selectedWorkflowId && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-lg">
            <h2 className="text-xl font-bold mb-4">执行工作流: {selectedWorkflowId}</h2>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">输入 Context (JSON)</label>
              <textarea
                value={executeContext}
                onChange={(e) => setExecuteContext(e.target.value)}
                className="w-full rounded-md border border-gray-300 px-3 py-2 h-32"
                placeholder='{"data": {"value": 100}}'
              />
            </div>
            <div className="flex justify-end space-x-2 mb-4">
              <button
                onClick={() => handleExecute(selectedWorkflowId)}
                className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700"
              >
                执行
              </button>
              <button
                onClick={() => setSelectedWorkflowId(null)}
                className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md"
              >
                关闭
              </button>
            </div>
            {executeResult && (
              <div className="bg-gray-50 p-4 rounded-md">
                <h3 className="text-sm font-medium text-gray-700 mb-2">结果:</h3>
                <pre className="text-xs overflow-auto">{JSON.stringify(executeResult, null, 2)}</pre>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}