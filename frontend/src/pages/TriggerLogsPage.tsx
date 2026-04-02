import { useState, useEffect } from 'react';
import { getTriggerLogs, TriggerExecutionLog } from '../api/workflow';

export function TriggerLogsPage() {
  const [logs, setLogs] = useState<TriggerExecutionLog[]>([]);
  const [workflowId, setWorkflowId] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadLogs();
  }, []);

  const loadLogs = async () => {
    setLoading(true);
    try {
      const data = await getTriggerLogs(workflowId || undefined);
      setLogs(data);
    } catch (error) {
      console.error('Failed to load trigger logs:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    loadLogs();
  };

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">触发历史</h1>

      <form onSubmit={handleSearch} className="mb-6 flex gap-4">
        <input
          type="text"
          value={workflowId}
          onChange={(e) => setWorkflowId(e.target.value)}
          placeholder="工作流 ID (可选)"
          className="flex-1 border rounded px-3 py-2"
        />
        <button
          type="submit"
          className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
        >
          查询
        </button>
      </form>

      {loading ? (
        <div className="text-center py-8 text-gray-500">加载中...</div>
      ) : (
        <div className="space-y-4">
          {logs.map((log) => (
            <div
              key={log.id}
              className={`p-4 border rounded ${
                log.status === 'SUCCESS' ? 'border-green-200 bg-green-50' :
                log.status === 'FAILED' ? 'border-red-200 bg-red-50' :
                'border-gray-200'
              }`}
            >
              <div className="flex justify-between items-start">
                <div>
                  <span className="font-medium">{log.workflowId}</span>
                  <span className="ml-3 text-sm text-gray-500">
                    {log.triggerType === 'SCHEDULE' ? '定时' :
                     log.triggerType === 'WEBHOOK' ? 'Webhook' : '手动'}
                  </span>
                </div>
                <span className="text-sm text-gray-500">
                  {new Date(log.triggeredAt).toLocaleString()}
                </span>
              </div>
              <div className="mt-2 text-sm text-gray-600">
                触发源: {log.triggerSource}
              </div>
              {log.error && (
                <div className="mt-2 text-sm text-red-600">
                  错误: {log.error}
                </div>
              )}
            </div>
          ))}
          {logs.length === 0 && (
            <div className="text-center py-8 text-gray-500">暂无数据</div>
          )}
        </div>
      )}
    </div>
  );
}
