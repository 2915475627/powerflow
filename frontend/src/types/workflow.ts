export interface WorkflowNode {
  id: string;
  name?: string;
  type: string;
  config?: Record<string, unknown>;
  inputMapping?: Record<string, string>;
  outputMapping?: Record<string, string>;
}

export interface WorkflowEdge {
  id: string;
  fromNodeId: string;
  toNodeId: string;
  condition?: string;
}

export interface Workflow {
  id: string;
  name: string;
  description?: string;
  nodes: Record<string, WorkflowNode>;
  edges: WorkflowEdge[];
  startNodeId: string;
}

export interface Context {
  data?: Record<string, unknown>;
  [key: string]: unknown;
}

export interface WorkflowExecutionResult {
  executionId: string;
  workflowId: string;
  status: 'SUCCESS' | 'FAILURE';
  finalContext: Context;
  nodeExecutions: NodeExecution[];
  error?: string;
  success: boolean;
}

export interface NodeExecution {
  id: string;
  workflowExecutionId: string;
  nodeId: string;
  status: 'SUCCESS' | 'FAILURE';
  input: Record<string, unknown>;
  output: Record<string, unknown>;
  error?: string;
  durationMs: number;
  startTime: string;
  endTime: string;
}

export interface NodeResult {
  nodeId: string;
  output: Record<string, unknown>;
  error?: string;
  success?: boolean;
  status?: 'SUCCESS' | 'FAILURE';
  nextNodeId?: Record<string, string>;
}

export interface NodeTemplate {
  id: string;
  name: string;
  nodeType: string;
  config: Record<string, unknown>;
  active: boolean;
  remark?: string;
  website?: string;
}