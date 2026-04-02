package com.powerflow.workflow.adapter.inbound.rest;

import com.powerflow.workflow.domain.model.Context;
import com.powerflow.workflow.domain.model.Node;
import com.powerflow.workflow.domain.model.TriggerExecutionLog;
import com.powerflow.workflow.domain.model.Workflow;
import com.powerflow.workflow.domain.model.WorkflowExecutionResult;
import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import com.powerflow.workflow.domain.model.enums.NodeType;
import com.powerflow.workflow.domain.model.enums.TriggerType;
import com.powerflow.workflow.domain.port.outbound.TriggerExecutionLogRepository;
import com.powerflow.workflow.domain.port.outbound.WorkflowRepository;
import com.powerflow.workflow.domain.service.WorkflowExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final WorkflowRepository workflowRepository;
    private final TriggerExecutionLogRepository triggerLogRepository;
    private final WorkflowExecutor workflowExecutor;

    public WebhookController(WorkflowRepository workflowRepository,
                              TriggerExecutionLogRepository triggerLogRepository,
                              WorkflowExecutor workflowExecutor) {
        this.workflowRepository = workflowRepository;
        this.triggerLogRepository = triggerLogRepository;
        this.workflowExecutor = workflowExecutor;
    }

    @PostMapping("/{path}")
    public ResponseEntity<?> triggerWebhook(
            @PathVariable String path,
            @RequestBody Map<String, Object> payload,
            @RequestHeader Map<String, String> headers) {

        // 1. Find matching workflow
        Workflow targetWorkflow = null;
        for (Workflow wf : workflowRepository.findAll()) {
            if (!wf.isEnabled()) continue;
            Node startNode = wf.findNodeById(wf.getStartNodeId()).orElse(null);
            if (startNode != null && startNode.getType() == NodeType.START) {
                String webhookPath = (String) startNode.getConfig().get("webhookPath");
                if (path.equals(webhookPath)) {
                    targetWorkflow = wf;
                    break;
                }
            }
        }

        if (targetWorkflow == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. Build Context from payload using fieldMappings
        Context ctx = buildContextFromPayload(targetWorkflow, payload);

        // 3. Execute workflow
        try {
            WorkflowExecutionResult result = workflowExecutor.execute(targetWorkflow.getId(), ctx);

            triggerLogRepository.save(TriggerExecutionLog.builder()
                .id(UUID.randomUUID().toString())
                .workflowId(targetWorkflow.getId())
                .triggerType(TriggerType.WEBHOOK)
                .triggerSource(path)
                .status(result.getStatus())
                .executionId(result.getExecutionId())
                .triggeredAt(LocalDateTime.now())
                .error(result.getError())
                .build());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "executionId", result.getExecutionId(),
                "status", result.getStatus()
            ));
        } catch (Exception e) {
            triggerLogRepository.save(TriggerExecutionLog.builder()
                .id(UUID.randomUUID().toString())
                .workflowId(targetWorkflow.getId())
                .triggerType(TriggerType.WEBHOOK)
                .triggerSource(path)
                .status(ExecutionStatus.FAILED)
                .triggeredAt(LocalDateTime.now())
                .error(e.getMessage())
                .build());

            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @SuppressWarnings("unchecked")
    private Context buildContextFromPayload(Workflow workflow, Map<String, Object> payload) {
        Node startNode = workflow.findNodeById(workflow.getStartNodeId()).orElse(null);
        if (startNode == null) {
            return new Context(Map.of());
        }

        Map<String, Object> fieldMappings = (Map<String, Object>) startNode.getConfig().get("fieldMappings");
        if (fieldMappings == null || fieldMappings.isEmpty()) {
            return new Context(Map.of("input", payload));
        }

        Map<String, Object> ctxData = new HashMap<>();
        for (Map.Entry<String, Object> entry : fieldMappings.entrySet()) {
            String sourceField = entry.getKey();
            String targetPath = (String) entry.getValue();
            Object value = payload.get(sourceField);
            setNestedValue(ctxData, targetPath, value);
        }

        return new Context(ctxData);
    }

    @SuppressWarnings("unchecked")
    private void setNestedValue(Map<String, Object> map, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = map;
        for (int i = 0; i < parts.length - 1; i++) {
            current.computeIfAbsent(parts[i], k -> new HashMap<>());
            current = (Map<String, Object>) current.get(parts[i]);
        }
        current.put(parts[parts.length - 1], value);
    }
}
