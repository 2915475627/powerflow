import { WorkflowCanvas } from '../components/WorkflowCanvas';

export function WorkflowEditorPage() {
  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-900">工作流编辑器</h1>
        <div className="flex gap-4">
          <button className="px-4 py-2 bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300">
            保存
          </button>
          <button className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700">
            执行
          </button>
        </div>
      </div>
      <WorkflowCanvas />
      <div className="mt-4 text-sm text-gray-500">
        <p>拖拽节点到画布，连接节点创建工作流流程。</p>
        <p className="mt-1">双击节点编辑属性。</p>
      </div>
    </div>
  );
}