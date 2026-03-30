package com.powerflow.workflow.adapter.outbound.persistence;

import com.powerflow.workflow.adapter.outbound.persistence.entity.WorkflowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaWorkflowRepository extends JpaRepository<WorkflowEntity, String> {
}
