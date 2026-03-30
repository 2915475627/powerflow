import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { workflowApi, executionApi } from '../api/workflow';
import type { Workflow, Context } from '../types/workflow';

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