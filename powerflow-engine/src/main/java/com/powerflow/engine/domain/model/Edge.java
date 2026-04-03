package com.powerflow.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Edge {
    private final String id;
    private final String fromNodeId;
    private final String toNodeId;
    private final String condition;

    @JsonCreator
    public Edge(
            @JsonProperty("id") String id,
            @JsonProperty("fromNodeId") String fromNodeId,
            @JsonProperty("toNodeId") String toNodeId,
            @JsonProperty("condition") String condition) {
        this.id = id;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.condition = condition;
    }

    // For builder compatibility
    protected Edge(String id, String fromNodeId, String toNodeId, String condition, boolean builder) {
        this.id = id;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.condition = condition;
    }

    public String getId() { return id; }
    public String getFromNodeId() { return fromNodeId; }
    public String getToNodeId() { return toNodeId; }
    public boolean hasCondition() { return condition != null && !condition.isBlank(); }
    public String getCondition() { return condition; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String fromNodeId;
        private String toNodeId;
        private String condition;

        public Builder id(String id) { this.id = id; return this; }
        public Builder fromNodeId(String fromNodeId) { this.fromNodeId = fromNodeId; return this; }
        public Builder toNodeId(String toNodeId) { this.toNodeId = toNodeId; return this; }
        public Builder condition(String condition) { this.condition = condition; return this; }
        public Edge build() { return new Edge(id, fromNodeId, toNodeId, condition, true); }
    }
}
