package com.powerflow.workflow.adapter.outbound.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "edges")
public class EdgeEntity {

    @Id
    private String id;
    private String fromNodeId;
    private String toNodeId;
    private String condition;
    private String workflowId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFromNodeId() { return fromNodeId; }
    public void setFromNodeId(String fromNodeId) { this.fromNodeId = fromNodeId; }
    public String getToNodeId() { return toNodeId; }
    public void setToNodeId(String toNodeId) { this.toNodeId = toNodeId; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
}
