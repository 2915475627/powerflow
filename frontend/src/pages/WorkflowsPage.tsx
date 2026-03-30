import { Link } from 'react-router-dom';

export function WorkflowsPage() {
  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">工作流列表</h1>
      <div className="grid gap-4">
        <div className="p-4 bg-white rounded-lg shadow hover:shadow-md transition-shadow">
          <h2 className="text-lg font-semibold text-gray-800">数据处理流程</h2>
          <p className="text-sm text-gray-500 mt-1">描述: 数据清洗和转换工作流</p>
          <div className="mt-3 flex gap-2">
            <Link
              to="/editor"
              className="px-3 py-1 bg-indigo-600 text-white text-sm rounded-md hover:bg-indigo-700"
            >
              编辑
            </Link>
            <Link
              to="/test/1/1"
              className="px-3 py-1 bg-gray-200 text-gray-700 text-sm rounded-md hover:bg-gray-300"
            >
              测试
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}