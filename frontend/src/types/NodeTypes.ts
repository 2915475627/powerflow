export type NodeType =
  | 'START'
  | 'DATA_PROCESSING'
  | 'CONDITION'
  | 'HTTP_REQUEST'
  | 'LLM_CALL'
  | 'PARALLEL'
  | 'FOREACH'
  | 'BRANCH'
  | 'SUBWORKFLOW'
  | 'TRY_CATCH'
  | 'RETRY';
