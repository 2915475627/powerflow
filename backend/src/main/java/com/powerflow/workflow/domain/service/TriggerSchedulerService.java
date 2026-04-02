package com.powerflow.workflow.domain.service;

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
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.UUID;

@Service
public class TriggerSchedulerService {

    private final WorkflowRepository workflowRepository;
    private final TriggerExecutionLogRepository triggerLogRepository;
    private final WorkflowExecutor workflowExecutor;
    private final TaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public TriggerSchedulerService(WorkflowRepository workflowRepository,
                                   TriggerExecutionLogRepository triggerLogRepository,
                                   WorkflowExecutor workflowExecutor,
                                   TaskScheduler taskScheduler) {
        this.workflowRepository = workflowRepository;
        this.triggerLogRepository = triggerLogRepository;
        this.workflowExecutor = workflowExecutor;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void init() {
        refreshScheduledTasks();
    }

    public void refreshScheduledTasks() {
        scheduledTasks.values().forEach(f -> f.cancel(false));
        scheduledTasks.clear();

        workflowRepository.findByEnabled(true).forEach(workflow -> {
            Optional<Node> startNode = workflow.findNodeById(workflow.getStartNodeId());
            if (startNode.isPresent() && startNode.get().getType() == NodeType.START) {
                Map<String, Object> config = startNode.get().getConfig();
                String triggerType = (String) config.get("triggerType");
                if ("SCHEDULE".equals(triggerType)) {
                    String cron = (String) config.get("cron");
                    if (cron != null && !cron.isEmpty()) {
                        scheduleWorkflow(workflow.getId(), cron);
                    }
                }
            }
        });
    }

    public void scheduleWorkflow(String workflowId, String cronExpression) {
        ScheduledFuture<?> future = taskScheduler.schedule(
            () -> triggerScheduledWorkflow(workflowId),
            new CronTrigger(cronExpression)
        );
        scheduledTasks.put(workflowId, future);
    }

    public void unscheduleWorkflow(String workflowId) {
        ScheduledFuture<?> future = scheduledTasks.remove(workflowId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void triggerScheduledWorkflow(String workflowId) {
        workflowRepository.findById(workflowId).ifPresent(workflow -> {
            try {
                Context ctx = new Context(Map.of());
                WorkflowExecutionResult result = workflowExecutor.execute(workflowId, ctx);
                String cron = workflow.findNodeById(workflow.getStartNodeId())
                    .map(n -> (String) n.getConfig().get("cron"))
                    .orElse("");
                triggerLogRepository.save(TriggerExecutionLog.builder()
                    .id(UUID.randomUUID().toString())
                    .workflowId(workflowId)
                    .triggerType(TriggerType.SCHEDULE)
                    .triggerSource(cron)
                    .status(result.getStatus())
                    .executionId(result.getExecutionId())
                    .triggeredAt(LocalDateTime.now())
                    .error(result.getError())
                    .build());
            } catch (Exception e) {
                String cron = workflow.findNodeById(workflow.getStartNodeId())
                    .map(n -> (String) n.getConfig().get("cron"))
                    .orElse("");
                triggerLogRepository.save(TriggerExecutionLog.builder()
                    .id(UUID.randomUUID().toString())
                    .workflowId(workflowId)
                    .triggerType(TriggerType.SCHEDULE)
                    .triggerSource(cron)
                    .status(ExecutionStatus.FAILED)
                    .triggeredAt(LocalDateTime.now())
                    .error(e.getMessage())
                    .build());
            }
        });
    }
}
