package com.powerflow.workflow.adapter.outbound.persistence.entity;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "nodes")
public class NodeEntity {

    @Id
    private String id;

    private String name;
    private String type;

    @Convert(converter = MapToJsonConverter.class)
    private Map<String, Object> config = new HashMap<>();

    @Convert(converter = MapToJsonConverter.class)
    private Map<String, String> inputMapping = new HashMap<>();

    @Convert(converter = MapToJsonConverter.class)
    private Map<String, String> outputMapping = new HashMap<>();

    private String workflowId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }
    public Map<String, String> getInputMapping() { return inputMapping; }
    public void setInputMapping(Map<String, String> inputMapping) { this.inputMapping = inputMapping; }
    public Map<String, String> getOutputMapping() { return outputMapping; }
    public void setOutputMapping(Map<String, String> outputMapping) { this.outputMapping = outputMapping; }
    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
}
