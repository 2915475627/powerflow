package com.powerflow.web.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class NodeExecutionAspect {

    private static final Logger log = LoggerFactory.getLogger(NodeExecutionAspect.class);

    @Around("execution(* com.powerflow.engine.engine.WorkflowEngine.execute(..))")
    public Object logWorkflowExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String workflowId = extractWorkflowId(joinPoint.getArgs());
        long startTime = System.currentTimeMillis();

        log.info("Workflow execution started: workflowId={}", workflowId);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("Workflow execution completed: workflowId={}, durationMs={}", workflowId, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Workflow execution failed: workflowId={}, durationMs={}, error={}",
                workflowId, duration, e.getMessage());
            throw e;
        }
    }

    @Around("execution(* com.powerflow.engine.engine.NodeDispatcher.dispatch(..))")
    public Object logNodeExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String nodeId = extractNodeId(joinPoint.getArgs());
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Node execution completed: nodeId={}, durationMs={}", nodeId, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Node execution failed: nodeId={}, durationMs={}, error={}",
                nodeId, duration, e.getMessage());
            throw e;
        }
    }

    private String extractWorkflowId(Object[] args) {
        if (args != null && args.length > 0) {
            return args[0] != null ? args[0].toString() : "unknown";
        }
        return "unknown";
    }

    private String extractNodeId(Object[] args) {
        if (args != null && args.length > 0 && args[0] != null) {
            return args[0].toString();
        }
        return "unknown";
    }
}
