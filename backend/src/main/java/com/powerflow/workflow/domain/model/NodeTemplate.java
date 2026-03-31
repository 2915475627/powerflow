package com.powerflow.workflow.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.powerflow.workflow.domain.model.enums.NodeType;
import java.util.HashMap;
import java.util.Map;

public class NodeTemplate {
    private final String id;
    private final String name;
    private final NodeType nodeType;
    private final Map<String, Object> config;
    private final boolean active;
    private final String remark;
    private final String website;

    @JsonCreator
    public NodeTemplate(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("nodeType") NodeType nodeType,
            @JsonProperty("config") Map<String, Object> config,
            @JsonProperty("active") boolean active,
            @JsonProperty("remark") String remark,
            @JsonProperty("website") String website) {
        this.id = id;
        this.name = name;
        this.nodeType = nodeType;
        this.config = config != null ? new HashMap<>(config) : new HashMap<>();
        this.active = active;
        this.remark = remark;
        this.website = website;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public NodeType getNodeType() { return nodeType; }
    public Map<String, Object> getConfig() { return new HashMap<>(config); }
    public boolean isActive() { return active; }
    public String getRemark() { return remark; }
    public String getWebsite() { return website; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String name;
        private NodeType nodeType;
        private Map<String, Object> config = new HashMap<>();
        private boolean active = true;
        private String remark;
        private String website;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder nodeType(NodeType nodeType) { this.nodeType = nodeType; return this; }
        public Builder config(Map<String, Object> config) { this.config = config; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder remark(String remark) { this.remark = remark; return this; }
        public Builder website(String website) { this.website = website; return this; }
        public NodeTemplate build() { return new NodeTemplate(id, name, nodeType, config, active, remark, website); }
    }
}
