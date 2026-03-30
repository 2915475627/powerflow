package com.powerflow.workflow.aop;

import com.powerflow.workflow.domain.model.NodeExecution;
import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import com.powerflow.workflow.domain.port.outbound.ExecutionLogRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Aspect
@Component
public class NodeExecutionAspect {

    private final ExecutionLogRepository executionLogRepository;

    public NodeExecutionAspect(ExecutionLogRepository executionLogRepository) {
        this.executionLogRepository = executionLogRepository;
    }

    @Around("execution(* com.powerflow.workflow.domain.service.NodeExecutorService.execute(..))")
    public Object aroundNodeExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String nodeId = args.length > 0 ? ((com.powerflow.workflow.domain.model.Node) args[0]).getId() : "unknown";
        String executionId = "aspect-" + System.currentTimeMillis();

        LocalDateTime startTime = LocalDateTime.now();
        Object result = null;
        ExecutionStatus status = ExecutionStatus.SUCCESS;
        String error = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            status = ExecutionStatus.FAILED;
            error = t.getMessage();
            throw t;
        } finally {
            long durationMs = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();

            NodeExecution execution = NodeExecution.builder()
                .workflowExecutionId(executionId)
                .nodeId(nodeId)
                .status(status)
                .output(result != null ? java.util.Map.of("result", result) : java.util.Map.of())
                .error(error)
                .durationMs(durationMs)
                .startTime(startTime)
                .endTime(LocalDateTime.now())
                .build();

            executionLogRepository.save(execution);
        }
    }
}
