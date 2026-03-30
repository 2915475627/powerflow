package com.powerflow.workflow.adapter.outbound.persistence;

import com.powerflow.workflow.adapter.outbound.persistence.entity.NodeExecutionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaNodeExecutionRepository extends JpaRepository<NodeExecutionEntity, String> {
    Page<NodeExecutionEntity> findByWorkflowExecutionId(String workflowExecutionId, Pageable pageable);
}
