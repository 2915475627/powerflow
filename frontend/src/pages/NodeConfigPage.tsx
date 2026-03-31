import { useState } from 'react';
import { useNodeTemplatesByType } from '../hooks/useWorkflow';
import type { NodeTemplate } from '../types/workflow';

// LLM Template Form
function LLMTemplateForm({
  template,
  onSave,
  onCancel,
}: {
  template?: Partial<NodeTemplate>;
  onSave: (data: Partial<NodeTemplate>) => void;
  onCancel: () => void;
}) {
  const [formData, setFormData] = useState<Partial<NodeTemplate>>(
    template || {
      name: '',
      nodeType: 'LLM_CALL',
      config: {
        provider: 'openai',
        model: 'gpt-4',
        temperature: 0.7,
        maxTokens: 2000,
      },
      active: true,
      remark: '',
      website: '',
    }
  );

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSave(formData);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-3 p-4 bg-gray-50 rounded-lg">
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">名称</label>
          <input
            type="text"
            value={formData.name || ''}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            className="w-full border rounded-md px-2 py-1 text-sm"
            placeholder="OpenAI GPT-4"
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">供应商</label>
          <select
            value={(formData.config as any)?.provider || 'openai'}
            onChange={(e) =>
              setFormData({ ...formData, config: { ...formData.config, provider: e.target.value } })
            }
            className="w-full border rounded-md px-2 py-1 text-sm"
          >
            <option value="openai">OpenAI</option>
            <option value="anthropic">Anthropic</option>
            <option value="azure">Azure OpenAI</option>
          </select>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">请求地址</label>
          <input
            type="text"
            value={(formData.config as any)?.baseUrl || ''}
            onChange={(e) =>
              setFormData({ ...formData, config: { ...formData.config, baseUrl: e.target.value } })
            }
            className="w-full border rounded-md px-2 py-1 text-sm"
            placeholder="https://api.openai.com/v1"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">模型</label>
          <input
            type="text"
            value={(formData.config as any)?.model || 'gpt-4'}
            onChange={(e) =>
              setFormData({ ...formData, config: { ...formData.config, model: e.target.value } })
            }
            className="w-full border rounded-md px-2 py-1 text-sm"
            placeholder="gpt-4"
          />
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">API Key</label>
        <input
          type="password"
          value={(formData.config as any)?.apiKey || ''}
          onChange={(e) =>
            setFormData({ ...formData, config: { ...formData.config, apiKey: e.target.value } })
          }
          className="w-full border rounded-md px-2 py-1 text-sm"
          placeholder="sk-..."
        />
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Temperature</label>
          <input
            type="number"
            step="0.1"
            min="0"
            max="2"
            value={(formData.config as any)?.temperature ?? 0.7}
            onChange={(e) =>
              setFormData({
                ...formData,
                config: { ...formData.config, temperature: parseFloat(e.target.value) },
              })
            }
            className="w-full border rounded-md px-2 py-1 text-sm"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Max Tokens</label>
          <input
            type="number"
            value={(formData.config as any)?.maxTokens || 2000}
            onChange={(e) =>
              setFormData({
                ...formData,
                config: { ...formData.config, maxTokens: parseInt(e.target.value) },
              })
            }
            className="w-full border rounded-md px-2 py-1 text-sm"
          />
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">官网链接</label>
        <input
          type="text"
          value={formData.website || ''}
          onChange={(e) => setFormData({ ...formData, website: e.target.value })}
          className="w-full border rounded-md px-2 py-1 text-sm"
          placeholder="https://platform.openai.com"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">备注</label>
        <textarea
          value={formData.remark || ''}
          onChange={(e) => setFormData({ ...formData, remark: e.target.value })}
          className="w-full border rounded-md px-2 py-1 text-sm h-20"
          placeholder="主用模型，响应速度快"
        />
      </div>

      <div className="flex items-center">
        <input
          type="checkbox"
          id="active"
          checked={formData.active ?? true}
          onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
          className="w-4 h-4 mr-2"
        />
        <label htmlFor="active" className="text-sm text-gray-700">启用</label>
      </div>

      <div className="flex space-x-2 pt-2">
        <button
          type="submit"
          className="flex-1 px-3 py-1.5 bg-indigo-600 text-white rounded-md text-sm hover:bg-indigo-700"
        >
          保存
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="px-3 py-1.5 bg-gray-200 text-gray-700 rounded-md text-sm hover:bg-gray-300"
        >
          取消
        </button>
      </div>
    </form>
  );
}

// HTTP Template Form
function HTTPTemplateForm({
  template,
  onSave,
  onCancel,
}: {
  template?: Partial<NodeTemplate>;
  onSave: (data: Partial<NodeTemplate>) => void;
  onCancel: () => void;
}) {
  const [formData, setFormData] = useState<Partial<NodeTemplate>>(
    template || {
      name: '',
      nodeType: 'HTTP_REQUEST',
      config: {
        url: 'https://api.example.com',
        method: 'GET',
        headers: {},
        timeout: 30000,
      },
      active: true,
      remark: '',
    }
  );

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSave(formData);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-3 p-4 bg-gray-50 rounded-lg">
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">名称</label>
          <input
            type="text"
            value={formData.name || ''}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            className="w-full border rounded-md px-2 py-1 text-sm"
            placeholder="Weather API"
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Method</label>
          <select
            value={(formData.config as any)?.method || 'GET'}
            onChange={(e) =>
              setFormData({ ...formData, config: { ...formData.config, method: e.target.value } })
            }
            className="w-full border rounded-md px-2 py-1 text-sm"
          >
            <option value="GET">GET</option>
            <option value="POST">POST</option>
            <option value="PUT">PUT</option>
            <option value="DELETE">DELETE</option>
            <option value="PATCH">PATCH</option>
          </select>
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">URL</label>
        <input
          type="text"
          value={(formData.config as any)?.url || ''}
          onChange={(e) =>
            setFormData({ ...formData, config: { ...formData.config, url: e.target.value } })
          }
          className="w-full border rounded-md px-2 py-1 text-sm"
          placeholder="https://api.example.com/endpoint"
          required
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Headers (JSON)</label>
        <textarea
          value={JSON.stringify((formData.config as any)?.headers || {}, null, 2)}
          onChange={(e) => {
            try {
              setFormData({ ...formData, config: { ...formData.config, headers: JSON.parse(e.target.value) } });
            } catch {}
          }}
          className="w-full border rounded-md px-2 py-1 text-sm h-20"
          placeholder='{"Content-Type": "application/json"}'
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Body (JSON)</label>
        <textarea
          value={typeof (formData.config as any)?.body === 'string'
            ? (formData.config as any)?.body
            : JSON.stringify((formData.config as any)?.body || {}, null, 2)}
          onChange={(e) => {
            try {
              setFormData({ ...formData, config: { ...formData.config, body: JSON.parse(e.target.value) } });
            } catch {
              setFormData({ ...formData, config: { ...formData.config, body: e.target.value } });
            }
          }}
          className="w-full border rounded-md px-2 py-1 text-sm h-20"
          placeholder='{"key": "value"}'
        />
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">超时 (ms)</label>
          <input
            type="number"
            value={(formData.config as any)?.timeout || 30000}
            onChange={(e) =>
              setFormData({
                ...formData,
                config: { ...formData.config, timeout: parseInt(e.target.value) },
              })
            }
            className="w-full border rounded-md px-2 py-1 text-sm"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">备注</label>
          <input
            type="text"
            value={formData.remark || ''}
            onChange={(e) => setFormData({ ...formData, remark: e.target.value })}
            className="w-full border rounded-md px-2 py-1 text-sm"
            placeholder="备注信息"
          />
        </div>
      </div>

      <div className="flex items-center">
        <input
          type="checkbox"
          id="active"
          checked={formData.active ?? true}
          onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
          className="w-4 h-4 mr-2"
        />
        <label htmlFor="active" className="text-sm text-gray-700">启用</label>
      </div>

      <div className="flex space-x-2 pt-2">
        <button
          type="submit"
          className="flex-1 px-3 py-1.5 bg-indigo-600 text-white rounded-md text-sm hover:bg-indigo-700"
        >
          保存
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="px-3 py-1.5 bg-gray-200 text-gray-700 rounded-md text-sm hover:bg-gray-300"
        >
          取消
        </button>
      </div>
    </form>
  );
}

// Template Card
function TemplateCard({
  template,
  onEdit,
  onDelete,
}: {
  template: NodeTemplate;
  onEdit: () => void;
  onDelete: () => void;
}) {
  return (
    <div className="border rounded-lg p-3 bg-white hover:shadow-md transition-shadow">
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <span className="font-medium text-gray-900">{template.name}</span>
          <span
            className={`text-xs px-1.5 py-0.5 rounded ${
              template.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
            }`}
          >
            {template.active ? '启用' : '禁用'}
          </span>
        </div>
        <div className="flex space-x-1">
          <button
            onClick={onEdit}
            className="px-2 py-1 text-xs text-indigo-600 hover:bg-indigo-50 rounded"
          >
            编辑
          </button>
          <button
            onClick={onDelete}
            className="px-2 py-1 text-xs text-red-600 hover:bg-red-50 rounded"
          >
            删除
          </button>
        </div>
      </div>
      {template.remark && (
        <p className="text-xs text-gray-500 mt-1">{template.remark}</p>
      )}
      <div className="text-xs text-gray-400 mt-1">
        {template.nodeType === 'LLM_CALL' && (
          <span>{(template.config as any)?.provider || 'openai'} / {(template.config as any)?.model || 'gpt-4'}</span>
        )}
        {template.nodeType === 'HTTP_REQUEST' && (
          <span>{(template.config as any)?.method || 'GET'} {(template.config as any)?.url}</span>
        )}
      </div>
    </div>
  );
}

// Template Group
function TemplateGroup({
  title,
  templates,
  onDelete,
  formComponent: FormComponent,
}: {
  title: string;
  templates: NodeTemplate[];
  onDelete: (template: NodeTemplate) => void;
  formComponent: React.ComponentType<{
    template?: Partial<NodeTemplate>;
    onSave: (data: Partial<NodeTemplate>) => void;
    onCancel: () => void;
  }>;
}) {
  const [isExpanded, setIsExpanded] = useState(true);
  const [editingTemplate, setEditingTemplate] = useState<Partial<NodeTemplate> | null>(null);
  const [showForm, setShowForm] = useState(false);

  const handleSave = (data: Partial<NodeTemplate>) => {
    // In real app, call API to save
    console.log('Saving template:', data);
    setShowForm(false);
    setEditingTemplate(null);
  };

  return (
    <div className="border rounded-lg overflow-hidden">
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        className="w-full px-4 py-3 bg-gray-50 flex items-center justify-between text-left"
      >
        <span className="font-medium text-gray-900">{title}</span>
        <div className="flex items-center space-x-2">
          <span className="text-xs text-gray-500">{templates.length} 个模板</span>
          <span className={`transform transition-transform ${isExpanded ? 'rotate-180' : ''}`}>
            ▼
          </span>
        </div>
      </button>

      {isExpanded && (
        <div className="p-4 space-y-3">
          {showForm ? (
            <FormComponent
              template={editingTemplate || undefined}
              onSave={handleSave}
              onCancel={() => {
                setShowForm(false);
                setEditingTemplate(null);
              }}
            />
          ) : (
            <>
              <div className="space-y-2">
                {templates.map((template) => (
                  <TemplateCard
                    key={template.id}
                    template={template}
                    onEdit={() => {
                      setEditingTemplate(template);
                      setShowForm(true);
                    }}
                    onDelete={() => onDelete(template)}
                  />
                ))}
              </div>
              <button
                onClick={() => {
                  setEditingTemplate(null);
                  setShowForm(true);
                }}
                className="w-full px-3 py-2 bg-indigo-100 text-indigo-700 rounded-md text-sm hover:bg-indigo-200"
              >
                + 新建模板
              </button>
            </>
          )}
        </div>
      )}
    </div>
  );
}

export function NodeConfigPage() {
  const { data: llmTemplates } = useNodeTemplatesByType('LLM_CALL');
  const { data: httpTemplates } = useNodeTemplatesByType('HTTP_REQUEST');

  const handleDelete = (template: NodeTemplate) => {
    if (confirm(`确定删除模板 "${template.name}" 吗？`)) {
      // In real app, call API to delete
      console.log('Deleting template:', template.id);
    }
  };

  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">节点配置管理</h1>

      <div className="space-y-4">
        <TemplateGroup
          title="LLM 调用"
          templates={llmTemplates || []}
          onDelete={handleDelete}
          formComponent={LLMTemplateForm}
        />

        <TemplateGroup
          title="HTTP 请求"
          templates={httpTemplates || []}
          onDelete={handleDelete}
          formComponent={HTTPTemplateForm}
        />
      </div>
    </div>
  );
}
