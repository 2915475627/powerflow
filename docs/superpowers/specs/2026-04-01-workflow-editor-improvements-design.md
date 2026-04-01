# Workflow Editor Improvements Design

> **Goal:** Fix three issues: (1) START node not visible in editor palette, (2) No manual trigger UI for webhook/scheduled tasks, (3) Missing workflow validation on save

## Context

The workflow editor currently has three issues:
1. The START node type is defined in `NodeConfigPanel.tsx` but not registered in ReactFlow's `nodeTypes`
2. No UI exists to manually trigger webhook/scheduled workflows
3. No validation exists when saving workflows (duplicate START nodes, duplicate node names)

## Design

### 1. Add START Node to ReactFlow Node Palette

**Create:** `frontend/src/components/nodes/StartNode.tsx`
```tsx
// Simple rounded rectangle with "开始" label
```

**Modify:** `frontend/src/pages/WorkflowEditorPage.tsx`
- Import and register `start` in `nodeTypes` map
- Add "开始节点" to the right panel palette under "触发类" group

**Modify:** `frontend/src/components/NodeConfigPanel.tsx`
- Update node palette to show grouped categories:
  - **触发类**：START
  - **数据处理**：DATA_PROCESSING, CONDITION
  - **集成**：HTTP_REQUEST, LLM_CALL
  - **控制流**：PARALLEL, FOREACH, BRANCH, TRY_CATCH, RETRY
  - **子工作流**：SUBWORKFLOW

### 2. START Node Manual Trigger UI

**Modify:** `frontend/src/components/NodeConfigPanel.tsx` - `STARTNodeConfigPanel`

Add a "手动触发" button at the bottom of START node config panel:
- When clicked, calls `POST /api/workflows/{workflowId}/execute` with empty context
- Shows success/failure toast notification
- Button is disabled if workflow is not enabled or triggerType is NONE

### 3. Workflow Save Validation (Double Validation)

#### Frontend Validation (Before Save)

In `WorkflowEditorPage.tsx` `saveWorkflow` function:

| Validator | Check | Error Message |
|-----------|-------|---------------|
| `WorkflowNameUniquenessValidator` | Workflow name must be unique | "工作流名称 '{name}' 已存在" |
| `StartNodeUniquenessValidator` | Only 1 START node allowed | "工作流只能有一个开始节点" |
| `NodeNameUniquenessValidator` | All node names must be unique | "节点名称 '{name}' 已重复" |
| `WorkflowStructureValidator` | At least 1 node exists | "工作流至少需要一个节点" |

On validation error: Show alert, block save.

#### Backend Validation (On Save)

**Create:** `backend/src/main/java/com/powerflow/workflow/domain/service/validation/WorkflowValidationChain.java`
```java
public interface WorkflowValidator {
    void validate(Workflow workflow);
}
```

**Create validators:**
- `WorkflowNameUniquenessValidator` - Checks workflow name is unique (excluding current workflow on update)
- `StartNodeUniquenessValidator` - Checks only 1 START node exists
- `NodeNameUniquenessValidator` - Checks all node names are unique
- `WorkflowStructureValidator` - Checks basic structure

**Create:** `WorkflowValidationException.java` - exception with error details

**Modify:** `WorkflowService.save()` - execute validation chain before saving

**Modify:** `WorkflowController` - catch validation exception and return 400 with errors

## API Changes

### New Error Response

```json
{
  "success": false,
  "error": "VALIDATION_ERROR",
  "details": [
    { "field": "name", "message": "工作流名称 '订单处理' 已存在" },
    { "field": "nodes", "message": "工作流只能有一个开始节点" },
    { "field": "nodes['calc'].name", "message": "节点名称 '计算' 已重复" }
  ]
}
```

## Implementation Order

1. Add START node to ReactFlow palette + grouped categories
2. Add manual trigger button to START node config panel
3. Add frontend validation in saveWorkflow
4. Add backend validation chain + exception handling
