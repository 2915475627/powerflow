export interface Workflow {
  id: string;
  name: string;
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
  createdAt: string;
  updatedAt: string;
}

export interface WorkflowNode {
  id: string;
  type: string;
  data: Record<string, unknown>;
  position: { x: number; y: number };
}

export interface WorkflowEdge {
  id: string;
  source: string;
  target: string;
}

export interface Context {
  [key: string]: unknown;
}

export interface WorkflowExecutionResult {
  executionId: string;
  workflowId: string;
  status: 'SUCCESS' | 'FAILURE';
  nodeExecutions: NodeExecution[];
  startedAt: string;
  completedAt: string;
  error?: string;
}

export interface NodeExecution {
  id: string;
  nodeId: string;
  status: 'SUCCESS' | 'FAILURE';
  startTime: string;
  durationMs: number;
  error?: string;
}

export interface NodeResult {
  nodeId: string;
  output: unknown;
  error?: string;
}