export type NodeType = 'DATA_PROCESSING' | 'CONDITION';
export type ExecutionStatus = 'SUCCESS' | 'FAILED';

export interface Context {
  data: Record<string, unknown>;
}

export interface Node {
  id: string;
  name: string;
  type: NodeType;
  config: Record<string, unknown>;
  inputMapping: Record<string, string>;
  outputMapping: Record<string, string>;
}

export interface Edge {
  id: string;
  fromNodeId: string;
  toNodeId: string;
  condition?: string;
}

export interface Workflow {
  id: string;
  name: string;
  description: string;
  nodes: Node[];
  edges: Edge[];
  startNodeId: string;
}

export interface NodeExecution {
  id: string;
  workflowExecutionId: string;
  nodeId: string;
  status: ExecutionStatus;
  input: Record<string, unknown>;
  output: Record<string, unknown>;
  error?: string;
  durationMs: number;
  startTime: string;
  endTime: string;
}

export interface WorkflowExecutionResult {
  workflowId: string;
  executionId: string;
  status: ExecutionStatus;
  finalContext: Context;
  nodeExecutions: NodeExecution[];
  error?: string;
}

export interface NodeResult {
  nodeId: string;
  status: ExecutionStatus;
  output: Record<string, unknown>;
  error?: string;
  nextNodeId?: string;
}