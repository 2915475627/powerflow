package com.powerflow.workflow.adapter.outbound.persistence;

import com.powerflow.workflow.adapter.outbound.persistence.entity.*;
import com.powerflow.workflow.domain.model.Workflow;
import com.powerflow.workflow.domain.port.outbound.WorkflowRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "persistence.type", havingValue = "jpa")
public class PostgresWorkflowRepository implements WorkflowRepository {

    private final JpaWorkflowRepository jpaRepository;
    private final EntityMapper mapper;

    public PostgresWorkflowRepository(JpaWorkflowRepository jpaRepository, EntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Optional<Workflow> findById(String id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Workflow save(Workflow workflow) {
        WorkflowEntity entity = mapper.toEntity(workflow);

        List<NodeEntity> nodeEntities = workflow.getNodes().values().stream()
            .map(n -> mapper.toNodeEntity(n, workflow.getId()))
            .toList();
        entity.setNodes(nodeEntities);

        List<EdgeEntity> edgeEntities = workflow.getEdges().stream()
            .map(e -> mapper.toEdgeEntity(e, workflow.getId()))
            .toList();
        entity.setEdges(edgeEntities);

        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(String id) {
        jpaRepository.deleteById(id);
    }
}
