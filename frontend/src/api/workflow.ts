import axios from 'axios';
import type { Workflow, Context, WorkflowExecutionResult, NodeResult } from '../types/workflow';

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