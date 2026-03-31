import axios from 'axios';
import type { Workflow, Context, WorkflowExecutionResult, NodeResult, NodeTemplate } from '../types/workflow';

const api = axios.create({
  baseURL: '/api',
});

export const workflowApi = {
  list: async (): Promise<Workflow[]> => {
    const response = await api.get('/workflows');
    return response.data;
  },

  get: async (id: string): Promise<Workflow> => {
    const response = await api.get(`/workflows/${id}`);
    return response.data;
  },

  create: async (workflow: Workflow): Promise<Workflow> => {
    const response = await api.post('/workflows', workflow);
    return response.data;
  },

  delete: async (id: string): Promise<void> => {
    await api.delete(`/workflows/${id}`);
  },

  execute: async (workflowId: string, context: Context): Promise<WorkflowExecutionResult> => {
    const response = await api.post(`/workflows/${workflowId}/execute`, context);
    return response.data;
  },

  testNode: async (workflowId: string, nodeId: string, context: Context): Promise<NodeResult> => {
    const response = await api.post(`/workflows/${workflowId}/nodes/${nodeId}/test`, context);
    return response.data;
  },
};

export const executionApi = {
  listByWorkflow: async (workflowId: string): Promise<WorkflowExecutionResult[]> => {
    const response = await api.get(`/executions/workflow/${workflowId}`);
    return response.data;
  },

  get: async (executionId: string): Promise<WorkflowExecutionResult> => {
    const response = await api.get(`/executions/${executionId}`);
    return response.data;
  },
};

export const nodeTemplateApi = {
  listAll: async (): Promise<NodeTemplate[]> => {
    const response = await api.get('/node-templates');
    return response.data;
  },

  getById: async (id: string): Promise<NodeTemplate> => {
    const response = await api.get(`/node-templates/${id}`);
    return response.data;
  },

  listByNodeType: async (nodeType: string): Promise<NodeTemplate[]> => {
    const response = await api.get(`/node-templates/type/${nodeType}`);
    return response.data;
  },

  create: async (template: NodeTemplate): Promise<NodeTemplate> => {
    const response = await api.post('/node-templates', template);
    return response.data;
  },

  update: async (id: string, template: NodeTemplate): Promise<NodeTemplate> => {
    const response = await api.put(`/node-templates/${id}`, template);
    return response.data;
  },

  delete: async (id: string): Promise<void> => {
    await api.delete(`/node-templates/${id}`);
  },
};