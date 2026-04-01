# Parallel Join Execution Design

## Overview

Design for implementing parallel execution with automatic join detection in the workflow engine. Uses graph edges to define parallel branches rather than declarative configuration.

## Architecture

### Workflow Structure

```
START → PARALLEL → [A] ─┬→ JOIN → END
                 └→ [B] ─┘
```

- **PARALLEL**: Entry point for parallel execution, outgoing edges define branches
- **Branch nodes (A, B)**: Regular nodes executed in parallel
- **JOIN**: Automatic join point that waits for all incoming branches before continuing

### Key Principles

1. **Edge-driven**: Branches defined by outgoing edges from PARALLEL, not config
2. **Auto-detection**: JOIN nodes detected automatically by having multiple incoming edges
3. **Strategy-based**: AND (all must succeed) or OR (any can succeed)

## Execution Flow

### ParallelHandler Execution

1. Identify branch nodes: all nodes reachable directly from PARALLEL via outgoing edges
2. Execute branches in parallel using ForkJoinPool
3. Collect results based on strategy:
   - **AND**: Wait for all, fail if any fails
   - **OR**: Return when first succeeds
4. Return join node ID as nextNodeId

### JOIN Node Behavior

1. JOIN node is identified by having multiple incoming edges
2. WorkflowExecutor tracks pending branches per JOIN
3. When a branch completes, decrement pending counter
4. When counter reaches 0, proceed to JOIN's nextNodeId

### Example Execution

```
1. START executes, nextNodeId = "parallel"
2. PARALLEL executes:
   - Identifies branches: [A, B]
   - strategy = AND
   - Executes A and B in parallel
   - A completes successfully
   - B completes successfully
   - Returns nextNodeId = "join"
3. WorkflowExecutor enters JOIN:
   - Checks pending branches = 2
   - A completes → pending = 1 (JOIN waits)
   - B completes → pending = 0 → proceed to END
4. END executes, workflow completes
```

## Data Model

### ParallelNode Config

```json
{
  "strategy": "AND",
  "branchNodeIds": ["A", "B"]
}
```

- `strategy`: "AND" or "OR"
- `branchNodeIds`: populated automatically from edges on save

### Edge-Based Branch Detection

```java
// ParallelHandler.java
List<Node> findBranches(Workflow workflow, String parallelNodeId) {
    return workflow.getEdges().stream()
        .filter(e -> e.getFromNodeId().equals(parallelNodeId))
        .map(e -> workflow.findNodeById(e.getToNodeId()).orElse(null))
        .filter(Objects::nonNull)
        .toList();
}
```

### JOIN Detection

```java
// JoinDetector.java
boolean isJoinNode(Workflow workflow, String nodeId) {
    long incomingEdges = workflow.getEdges().stream()
        .filter(e -> e.getToNodeId().equals(nodeId))
        .count();
    return incomingEdges > 1;
}
```

## Implementation Tasks

### Backend

1. **ParallelHandler** - Implement actual parallel execution
   - Find branches from outgoing edges
   - Execute using ForkJoinPool
   - Implement AND/OR strategy

2. **WorkflowExecutor** - Add JOIN handling
   - Track pending branches per JOIN
   - Wait for all branches before proceeding

3. **JoinDetector** - Utility to detect JOIN nodes
   - Identify nodes with multiple incoming edges

4. **Workflow structure validation**
   - Verify branches eventually converge to a JOIN
   - Verify no orphan branches

### Frontend

1. **Auto-populate branches** - On save, compute branchNodeIds from edges
2. **Visual feedback** - Show which nodes are JOIN nodes
3. **Validation** - Warn if parallel branches don't converge

## Error Handling

- **Branch failure with AND strategy**: Mark parallel as failed, stop workflow
- **Branch failure with OR strategy**: Continue waiting for other branches or return failure
- **No JOIN convergence**: Validation error on save
- **Circular dependencies**: Detection and prevention

## Validation Rules

1. PARALLEL must have at least 2 outgoing edges (2+ branches)
2. PARALLEL branches must converge to exactly one JOIN node
3. JOIN must have exactly 1 outgoing edge (one continuation path)
4. No circular dependencies in parallel structure
