import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { workflowApi, executionApi, nodeTemplateApi } from '../api/workflow';
import type { Workflow, Context, NodeTemplate } from '../types/workflow';

export function useWorkflows() {
  return useQuery({
    queryKey: ['workflows'],
    queryFn: workflowApi.list,
  });
}

export function useWorkflow(id: string) {
  return useQuery({
    queryKey: ['workflows', id],
    queryFn: () => workflowApi.get(id),
    enabled: !!id,
  });
}

export function useCreateWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (workflow: Workflow) => workflowApi.create(workflow),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['workflows'] });
    },
  });
}

export function useDeleteWorkflow() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => workflowApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['workflows'] });
    },
  });
}

export function useExecuteWorkflow() {
  return useMutation({
    mutationFn: ({ workflowId, context }: { workflowId: string; context: Context }) =>
      workflowApi.execute(workflowId, context),
  });
}

export function useTestNode() {
  return useMutation({
    mutationFn: ({ workflowId, nodeId, context }: { workflowId: string; nodeId: string; context: Context }) =>
      workflowApi.testNode(workflowId, nodeId, context),
  });
}

export function useExecutionHistory(workflowId: string) {
  return useQuery({
    queryKey: ['execution-history', workflowId],
    queryFn: () => executionApi.listByWorkflow(workflowId),
    enabled: !!workflowId,
  });
}

export function useNodeTemplates() {
  return useQuery({
    queryKey: ['node-templates'],
    queryFn: nodeTemplateApi.listAll,
  });
}

export function useNodeTemplatesByType(nodeType: string) {
  return useQuery({
    queryKey: ['node-templates', nodeType],
    queryFn: () => nodeTemplateApi.listByNodeType(nodeType),
    enabled: !!nodeType,
  });
}

export function useCreateNodeTemplate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (template: Omit<NodeTemplate, 'id'>) => nodeTemplateApi.create(template),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['node-templates'] });
    },
  });
}

export function useUpdateNodeTemplate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, template }: { id: string; template: NodeTemplate }) =>
      nodeTemplateApi.update(id, template),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['node-templates'] });
    },
  });
}

export function useDeleteNodeTemplate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => nodeTemplateApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['node-templates'] });
    },
  });
}