package com.powerflow.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powerflow.web.persistence.InMemoryExecutionLog;
import com.powerflow.web.persistence.InMemoryWorkflowRepository;
import com.powerflow.web.redis.RedisWorkflowLockAdapter;
import com.powerflow.engine.domain.port.outbound.ExecutionLogPort;
import com.powerflow.engine.domain.port.outbound.WorkflowRepositoryPort;
import com.powerflow.engine.engine.ContextManager;
import com.powerflow.engine.engine.NodeDispatcher;
import com.powerflow.engine.engine.WorkflowEngine;
import com.powerflow.engine.domain.port.inbound.WorkflowEnginePort;
import com.powerflow.engine.engine.WorkflowEngine;
import com.powerflow.engine.spi.NodeExecutorLoader;
import com.powerflow.engine.validation.ValidationChain;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PowerFlowConfig {

    @Bean
    public NodeDispatcher nodeDispatcher() {
        NodeDispatcher dispatcher = new NodeDispatcher();
        NodeExecutorLoader.loadNodesInto(dispatcher);
        return dispatcher;
    }

    @Bean
    public WorkflowRepositoryPort workflowRepositoryPort() {
        return new InMemoryWorkflowRepository();
    }

    @Bean
    public ExecutionLogPort executionLog() {
        return new InMemoryExecutionLog();
    }

    @Bean
    public ContextManager contextManager() {
        return new ContextManager();
    }

    @Bean
    public ValidationChain validationChain() {
        return new DefaultValidationChain();
    }

    @Bean
    public WorkflowEnginePort workflowEngine(
            WorkflowRepositoryPort workflowRepository,
            ExecutionLogPort executionLog,
            NodeDispatcher nodeDispatcher,
            ContextManager contextManager,
            ValidationChain validationChain) {
        return new WorkflowEngine(
            workflowRepository,
            executionLog,
            nodeDispatcher,
            contextManager,
            validationChain
        );
    }

    @Bean
    public RedisWorkflowLockAdapter redisWorkflowLockAdapter(org.springframework.data.redis.core.StringRedisTemplate redisTemplate) {
        return new RedisWorkflowLockAdapter(redisTemplate);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
