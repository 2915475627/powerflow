package com.powerflow.workflow.adapter.outbound.persistence.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workflows")
public class WorkflowEntity {

    @Id
    private String id;

    private String name;
    private String description;
    private String startNodeId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "workflow_id")
    private List<NodeEntity> nodes = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "workflow_id")
    private List<EdgeEntity> edges = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStartNodeId() { return startNodeId; }
    public void setStartNodeId(String startNodeId) { this.startNodeId = startNodeId; }
    public List<NodeEntity> getNodes() { return nodes; }
    public void setNodes(List<NodeEntity> nodes) { this.nodes = nodes; }
    public List<EdgeEntity> getEdges() { return edges; }
    public void setEdges(List<EdgeEntity> edges) { this.edges = edges; }
}
