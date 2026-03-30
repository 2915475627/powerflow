import { useState } from 'react';
import { useWorkflows, useDeleteWorkflow } from '../hooks/useWorkflow';
import type { Workflow } from '../types/workflow';

export function WorkflowsPage() {
  const { data: workflows, isLoading, error } = useWorkflows();
  const deleteWorkflow = useDeleteWorkflow();
  const [showForm, setShowForm] = useState(false);
  const [newWorkflow, setNewWorkflow] = useState<Partial<Workflow>>({
    name: '',
    description: '',
    nodes: [],
    edges: [],
    startNodeId: '',
  });

  const handleCreate = async () => {
    const { useCreateWorkflow } = await import('../hooks/useWorkflow');
    const createWorkflow = useCreateWorkflow();
    await createWorkflow.mutateAsync({
      id: crypto.randomUUID(),
      name: newWorkflow.name || '',
      description: newWorkflow.description || '',
      nodes: [],
      edges: [],
      startNodeId: '',
    });
    setShowForm(false);
    setNewWorkflow({ name: '', description: '', nodes: [], edges: [], startNodeId: '' });
  };

  const handleDelete = (id: string) => {
    if (confirm('确定要删除这个工作流吗？')) {
      deleteWorkflow.mutate(id);
    }
  };

  if (isLoading) return <div className="text-center py-10">加载中...</div>;
  if (error) return <div className="text-red-500 py-10">加载失败: {String(error)}</div>;

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-900">工作流管理</h1>
        <button
          onClick={() => setShowForm(true)}
          className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
        >
          创建工作流
        </button>
      </div>

      {showForm && (
        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <h2 className="text-lg font-semibold mb-4">新建工作流</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700">名称</label>
              <input
                type="text"
                value={newWorkflow.name}
                onChange={(e) => setNewWorkflow({ ...newWorkflow, name: e.target.value })}
                className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">描述</label>
              <textarea
                value={newWorkflow.description}
                onChange={(e) => setNewWorkflow({ ...newWorkflow, description: e.target.value })}
                className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2"
              />
            </div>
            <div className="flex space-x-4">
              <button
                onClick={handleCreate}
                className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700"
              >
                创建
              </button>
              <button
                onClick={() => setShowForm(false)}
                className="px-4 py-2 bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300"
              >
                取消
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                名称
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                描述
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                节点数
              </th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                操作
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {workflows?.map((workflow) => (
              <tr key={workflow.id}>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="text-sm font-medium text-gray-900">{workflow.name}</div>
                </td>
                <td className="px-6 py-4">
                  <div className="text-sm text-gray-500">{workflow.description}</div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {workflow.nodes?.length || 0}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <button
                    onClick={() => handleDelete(workflow.id)}
                    className="text-red-600 hover:text-red-900 mr-4"
                  >
                    删除
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {(!workflows || workflows.length === 0) && (
          <div className="text-center py-10 text-gray-500">暂无工作流</div>
        )}
      </div>
    </div>
  );
}