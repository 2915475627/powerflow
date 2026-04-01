import type { Node } from '@xyflow/react';

export type TriggerType = 'NONE' | 'SCHEDULE' | 'WEBHOOK';

export interface FieldMapping {
  fieldName: string;
  expression: string;
}

export interface StartNodeConfig {
  triggerType: TriggerType;
  cronExpression?: string;
  webhookPath?: string;
  fieldMappings?: FieldMapping[];
}

// FieldMappingsEditor helper component
function FieldMappingsEditor({
  value,
  onChange,
}: {
  value: FieldMapping[];
  onChange: (mappings: FieldMapping[]) => void;
}) {
  const handleAdd = () => {
    onChange([...value, { fieldName: '', expression: '' }]);
  };

  const handleRemove = (index: number) => {
    onChange(value.filter((_, i) => i !== index));
  };

  const handleUpdate = (index: number, field: keyof FieldMapping, newValue: string) => {
    const updated = value.map((item, i) =>
      i === index ? { ...item, [field]: newValue } : item
    );
    onChange(updated);
  };

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <label className="block text-sm font-medium text-gray-700">字段映射</label>
        <button
          type="button"
          onClick={handleAdd}
          className="text-xs text-indigo-600 hover:text-indigo-800"
        >
          + 添加映射
        </button>
      </div>
      {value.length === 0 ? (
        <div className="text-sm text-gray-400 italic">暂无字段映射</div>
      ) : (
        <div className="space-y-2">
          {value.map((mapping, index) => (
            <div key={index} className="flex gap-2 items-center">
              <input
                type="text"
                value={mapping.fieldName}
                onChange={(e) => handleUpdate(index, 'fieldName', e.target.value)}
                className="flex-1 border rounded-md px-2 py-1 text-sm"
                placeholder="字段名"
              />
              <input
                type="text"
                value={mapping.expression}
                onChange={(e) => handleUpdate(index, 'expression', e.target.value)}
                className="flex-1 border rounded-md px-2 py-1 text-sm"
                placeholder="#input.value"
              />
              <button
                type="button"
                onClick={() => handleRemove(index)}
                className="text-red-500 hover:text-red-700 text-sm"
              >
                x
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// STARTNodeConfigPanel component
function STARTNodeConfigPanel({
  config,
  onChange,
}: {
  config: StartNodeConfig;
  onChange: (c: StartNodeConfig) => void;
}) {
  const handleTriggerTypeChange = (triggerType: TriggerType) => {
    onChange({
      ...config,
      triggerType,
      cronExpression: triggerType === 'SCHEDULE' ? config.cronExpression || '0 * * * *' : undefined,
      webhookPath: triggerType === 'WEBHOOK' ? config.webhookPath || '/webhook' : undefined,
      fieldMappings: triggerType === 'WEBHOOK' ? config.fieldMappings || [] : config.fieldMappings,
    });
  };

  return (
    <div className="space-y-3">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">触发类型</label>
        <select
          value={config.triggerType || 'NONE'}
          onChange={(e) => handleTriggerTypeChange(e.target.value as TriggerType)}
          className="w-full border rounded-md px-2 py-1 text-sm"
        >
          <option value="NONE">无 (None)</option>
          <option value="SCHEDULE">定时调度 (Schedule)</option>
          <option value="WEBHOOK">Webhook</option>
        </select>
      </div>

      {config.triggerType === 'SCHEDULE' && (
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Cron 表达式</label>
          <input
            type="text"
            value={config.cronExpression || '0 * * * *'}
            onChange={(e) => onChange({ ...config, cronExpression: e.target.value })}
            className="w-full border rounded-md px-2 py-1 text-sm"
            placeholder="0 * * * *"
          />
          <div className="text-xs text-gray-500 mt-1">
            格式: 分 时 日 月 周 (例如: "0 * * * *" 表示每小时)
          </div>
        </div>
      )}

      {config.triggerType === 'WEBHOOK' && (
        <>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Webhook 路径</label>
            <input
              type="text"
              value={config.webhookPath || '/webhook'}
              onChange={(e) => onChange({ ...config, webhookPath: e.target.value })}
              className="w-full border rounded-md px-2 py-1 text-sm"
              placeholder="/webhook"
            />
            <div className="text-xs text-gray-500 mt-1">
              Webhook URL 将为: /api/webhooks/&lt;workflow-id&gt;/&lt;path&gt;
            </div>
          </div>
          <FieldMappingsEditor
            value={config.fieldMappings || []}
            onChange={(fieldMappings) => onChange({ ...config, fieldMappings })}
          />
        </>
      )}
    </div>
  );
}

// NodeConfigPanel main component
export function NodeConfigPanel({
  selectedNode,
  nodeConfig,
  onNodeConfigChange,
}: {
  selectedNode: Node | null;
  nodeConfig: Record<string, unknown>;
  onNodeConfigChange: (config: Record<string, unknown>) => void;
}) {
  if (!selectedNode) {
    return null;
  }

  const renderConfigPanel = () => {
    switch (selectedNode.type) {
      case 'start':
        return (
          <STARTNodeConfigPanel
            config={nodeConfig as StartNodeConfig}
            onChange={onNodeConfigChange}
          />
        );
      case 'DATA_PROCESSING':
        return (
          <>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">表达式</label>
              <input
                type="text"
                value={(nodeConfig as any).expression || ''}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, expression: e.target.value })}
                className="w-full border rounded-md px-2 py-1 text-sm"
                placeholder="#input.value * 2"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">输出 Key</label>
              <input
                type="text"
                value={(nodeConfig as any).outputKey || ''}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, outputKey: e.target.value })}
                className="w-full border rounded-md px-2 py-1 text-sm"
                placeholder="result"
              />
            </div>
          </>
        );
      case 'CONDITION':
        return (
          <>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">条件 (JSON)</label>
              <textarea
                value={JSON.stringify((nodeConfig as any).conditions || [], null, 2)}
                onChange={(e) => {
                  try {
                    onNodeConfigChange({ ...nodeConfig, conditions: JSON.parse(e.target.value) });
                  } catch {}
                }}
                className="w-full border rounded-md px-2 py-1 text-sm h-24"
                placeholder='[{"expression": "#input.value > 100", "nextNodeId": "node1"}]'
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">默认下一节点</label>
              <input
                type="text"
                value={(nodeConfig as any).defaultNextNodeId || ''}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, defaultNextNodeId: e.target.value })}
                className="w-full border rounded-md px-2 py-1 text-sm"
                placeholder="default-node"
              />
            </div>
          </>
        );
      case 'HTTP_REQUEST':
        return (
          <>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">URL</label>
              <input
                type="text"
                value={(nodeConfig as any).url || ''}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, url: e.target.value })}
                className="w-full border rounded-md px-2 py-1 text-sm"
                placeholder="https://api.example.com"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Method</label>
              <select
                value={(nodeConfig as any).method || 'GET'}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, method: e.target.value })}
                className="w-full border rounded-md px-2 py-1 text-sm"
              >
                <option value="GET">GET</option>
                <option value="POST">POST</option>
                <option value="PUT">PUT</option>
                <option value="DELETE">DELETE</option>
                <option value="PATCH">PATCH</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Body (JSON)</label>
              <textarea
                value={typeof (nodeConfig as any).body === 'string'
                  ? (nodeConfig as any).body
                  : JSON.stringify((nodeConfig as any).body || {}, null, 2)}
                onChange={(e) => {
                  try {
                    onNodeConfigChange({ ...nodeConfig, body: JSON.parse(e.target.value) });
                  } catch {
                    onNodeConfigChange({ ...nodeConfig, body: e.target.value });
                  }
                }}
                className="w-full border rounded-md px-2 py-1 text-sm h-20"
                placeholder='{"key": "value"}'
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">输出 Key</label>
              <input
                type="text"
                value={(nodeConfig as any).outputKey || 'httpResponse'}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, outputKey: e.target.value })}
                className="w-full border rounded-md px-2 py-1 text-sm"
                placeholder="httpResponse"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">超时 (ms)</label>
              <input
                type="number"
                value={(nodeConfig as any).timeout || 30000}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, timeout: parseInt(e.target.value) })}
                className="w-full border rounded-md px-2 py-1 text-sm"
              />
            </div>
          </>
        );
      case 'LLM_CALL':
        return (
          <>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Provider</label>
              <select
                value={(nodeConfig as any).provider || 'openai'}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, provider: e.target.value })}
                className="w-full border rounded-md px-2 py-1 text-sm"
              >
                <option value="openai">OpenAI</option>
                <option value="anthropic">Anthropic</option>
                <option value="azure">Azure OpenAI</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Model</label>
              <input
                type="text"
                value={(nodeConfig as any).model || 'gpt-4'}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, model: e.target.value })}
                className="w-full border rounded-md px-2 py-1 text-sm"
                placeholder="gpt-4"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Temperature</label>
              <input
                type="number"
                step="0.1"
                min="0"
                max="2"
                value={(nodeConfig as any).temperature ?? 0.7}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, temperature: parseFloat(e.target.value) })}
                className="w-full border rounded-md px-2 py-1 text-sm"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Max Tokens</label>
              <input
                type="number"
                value={(nodeConfig as any).maxTokens || 2000}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, maxTokens: parseInt(e.target.value) })}
                className="w-full border rounded-md px-2 py-1 text-sm"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Prompt</label>
              <textarea
                value={(nodeConfig as any).prompt || ''}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, prompt: e.target.value })}
                className="w-full border rounded-md px-2 py-1 text-sm h-24"
                placeholder="Please process #input.value"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">输出 Key</label>
              <input
                type="text"
                value={(nodeConfig as any).outputKey || 'llmResponse'}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, outputKey: e.target.value })}
                className="w-full border rounded-md px-2 py-1 text-sm"
                placeholder="llmResponse"
              />
            </div>
          </>
        );
      case 'PARALLEL':
        return (
          <>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">策略</label>
              <select
                value={(nodeConfig as any).strategy || 'AND'}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, strategy: e.target.value })}
                className="w-full border rounded-md px-2 py-1 text-sm"
              >
                <option value="AND">AND (全部完成)</option>
                <option value="OR">OR (任一完成)</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">分支 (JSON)</label>
              <textarea
                value={JSON.stringify((nodeConfig as any).branches || [], null, 2)}
                onChange={(e) => {
                  try {
                    onNodeConfigChange({ ...nodeConfig, branches: JSON.parse(e.target.value) });
                  } catch {}
                }}
                className="w-full border rounded-md px-2 py-1 text-sm h-32"
                placeholder='[{"name": "branch1", "nodeIds": ["n1", "n2"]}]'
              />
            </div>
          </>
        );
      case 'FOREACH':
        return (
          <>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">集合表达式</label>
              <input
                type="text"
                value={(nodeConfig as any).collection || '#input.items'}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, collection: e.target.value })}
                className="w-full border rounded-md px-2 py-1 text-sm"
                placeholder="#input.items"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">循环变量名</label>
              <input
                type="text"
                value={(nodeConfig as any).variableName || 'item'}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, variableName: e.target.value })}
                className="w-full border rounded-md px-2 py-1 text-sm"
                placeholder="item"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">最大迭代次数</label>
              <input
                type="number"
                value={(nodeConfig as any).maxIterations || 100}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, maxIterations: parseInt(e.target.value) })}
                className="w-full border rounded-md px-2 py-1 text-sm"
              />
            </div>
            <div className="flex items-center">
              <input
                type="checkbox"
                id="failOnError"
                checked={(nodeConfig as any).failOnError !== false}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, failOnError: e.target.checked })}
                className="w-4 h-4 mr-2"
              />
              <label htmlFor="failOnError" className="text-sm text-gray-700">失败时终止</label>
            </div>
          </>
        );
      case 'BRANCH':
        return (
          <>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">分支 (JSON)</label>
              <textarea
                value={JSON.stringify((nodeConfig as any).branches || [], null, 2)}
                onChange={(e) => {
                  try {
                    onNodeConfigChange({ ...nodeConfig, branches: JSON.parse(e.target.value) });
                  } catch {}
                }}
                className="w-full border rounded-md px-2 py-1 text-sm h-32"
                placeholder='[{"name": "high", "expression": "#input.value > 100", "nextNodeId": "n1"}]'
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">默认下一节点</label>
              <input
                type="text"
                value={(nodeConfig as any).defaultNextNodeId || ''}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, defaultNextNodeId: e.target.value })}
                className="w-full border rounded-md px-2 py-1 text-sm"
                placeholder="default-node"
              />
            </div>
          </>
        );
      case 'SUBWORKFLOW':
        return (
          <>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">工作流 ID</label>
              <input
                type="text"
                value={(nodeConfig as any).workflowId || ''}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, workflowId: e.target.value })}
                className="w-full border rounded-md px-2 py-1 text-sm"
                placeholder="sub-workflow-id"
              />
            </div>
          </>
        );
      case 'TRY_CATCH':
        return (
          <>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Try 节点 (JSON)</label>
              <textarea
                value={JSON.stringify((nodeConfig as any).tryNodes || [], null, 2)}
                onChange={(e) => {
                  try {
                    onNodeConfigChange({ ...nodeConfig, tryNodes: JSON.parse(e.target.value) });
                  } catch {}
                }}
                className="w-full border rounded-md px-2 py-1 text-sm h-24"
                placeholder='["node1", "node2"]'
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Catch 节点 (JSON)</label>
              <textarea
                value={JSON.stringify((nodeConfig as any).catchNodes || [], null, 2)}
                onChange={(e) => {
                  try {
                    onNodeConfigChange({ ...nodeConfig, catchNodes: JSON.parse(e.target.value) });
                  } catch {}
                }}
                className="w-full border rounded-md px-2 py-1 text-sm h-24"
                placeholder='["error_handler"]'
              />
            </div>
          </>
        );
      case 'RETRY':
        return (
          <>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">最大重试次数</label>
              <input
                type="number"
                value={(nodeConfig as any).maxAttempts || 3}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, maxAttempts: parseInt(e.target.value) })}
                className="w-full border rounded-md px-2 py-1 text-sm"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">初始延迟 (ms)</label>
              <input
                type="number"
                value={(nodeConfig as any).initialDelayMs || 1000}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, initialDelayMs: parseInt(e.target.value) })}
                className="w-full border rounded-md px-2 py-1 text-sm"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">最大延迟 (ms)</label>
              <input
                type="number"
                value={(nodeConfig as any).maxDelayMs || 30000}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, maxDelayMs: parseInt(e.target.value) })}
                className="w-full border rounded-md px-2 py-1 text-sm"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">退避策略</label>
              <select
                value={(nodeConfig as any).backoffStrategy || 'EXPONENTIAL'}
                onChange={(e) => onNodeConfigChange({ ...nodeConfig, backoffStrategy: e.target.value })}
                className="w-full border rounded-md px-2 py-1 text-sm"
              >
                <option value="FIXED">固定</option>
                <option value="EXPONENTIAL">指数</option>
                <option value="FIBONACCI">斐波那契</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">目标节点 (JSON)</label>
              <textarea
                value={JSON.stringify((nodeConfig as any).targetNodeIds || [], null, 2)}
                onChange={(e) => {
                  try {
                    onNodeConfigChange({ ...nodeConfig, targetNodeIds: JSON.parse(e.target.value) });
                  } catch {}
                }}
                className="w-full border rounded-md px-2 py-1 text-sm h-20"
                placeholder='["node1"]'
              />
            </div>
          </>
        );
      default:
        return (
          <div className="text-sm text-gray-500">
            暂不支持此节点类型的配置: {selectedNode.type}
          </div>
        );
    }
  };

  return (
    <div className="space-y-3">
      <h3 className="font-bold text-gray-900">节点配置</h3>
      {renderConfigPanel()}
    </div>
  );
}
