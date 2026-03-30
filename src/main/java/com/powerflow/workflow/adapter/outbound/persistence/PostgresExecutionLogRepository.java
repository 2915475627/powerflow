package com.powerflow.workflow.adapter.outbound.persistence;

import com.powerflow.workflow.adapter.outbound.persistence.entity.NodeExecutionEntity;
import com.powerflow.workflow.domain.model.NodeExecution;
import com.powerflow.workflow.domain.port.outbound.ExecutionLogRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@ConditionalOnProperty(name = "persistence.type", havingValue = "jpa")
public class PostgresExecutionLogRepository implements ExecutionLogRepository {

    private final JpaNodeExecutionRepository jpaRepository;
    private final EntityMapper mapper;

    public PostgresExecutionLogRepository(JpaNodeExecutionRepository jpaRepository, EntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void save(NodeExecution execution) {
        jpaRepository.save(mapper.toNodeExecutionEntity(execution));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NodeExecution> findByWorkflowExecutionId(String workflowExecutionId) {
        return jpaRepository.findByWorkflowExecutionId(workflowExecutionId, Pageable.unpaged())
            .getContent()
            .stream()
            .map(mapper::toNodeExecutionDomain)
            .toList();
    }
}
