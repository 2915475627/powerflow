package com.powerflow.workflow.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Workflow {
    private final String id;
    private final String name;
    private final String description;
    private final Map<String, Node> nodes;
    private final List<Edge> edges;
    private final String startNodeId;
    private final boolean enabled;

    @JsonCreator
    public Workflow(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("nodes") Map<String, Node> nodeMap,
            @JsonProperty("edges") List<Edge> edges,
            @JsonProperty("startNodeId") String startNodeId,
            @JsonProperty("enabled") Boolean enabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.nodes = nodeMap != null ? new HashMap<>(nodeMap) : new HashMap<>();
        this.edges = edges != null ? new ArrayList<>(edges) : new ArrayList<>();
        this.startNodeId = startNodeId;
        this.enabled = enabled != null ? enabled : false;
    }

    // Legacy constructor for Builder pattern
    protected Workflow(String id, String name, String description,
                       List<Node> nodesList, List<Edge> edges, String startNodeId,
                       boolean enabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.nodes = new HashMap<>();
        if (nodesList != null) {
            for (Node node : nodesList) {
                this.nodes.put(node.getId(), node);
            }
        }
        this.edges = edges != null ? new ArrayList<>(edges) : new ArrayList<>();
        this.startNodeId = startNodeId;
        this.enabled = enabled;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Map<String, Node> getNodes() { return new HashMap<>(nodes); }
    public List<Edge> getEdges() { return new ArrayList<>(edges); }
    public String getStartNodeId() { return startNodeId; }
    public boolean isEnabled() { return enabled; }

    public Optional<Node> findNodeById(String nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    public List<Edge> findEdgesByFromNodeId(String fromNodeId) {
        return edges.stream()
            .filter(e -> e.getFromNodeId().equals(fromNodeId))
            .toList();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String name;
        private String description;
        private List<Node> nodes = new ArrayList<>();
        private List<Edge> edges = new ArrayList<>();
        private String startNodeId;
        private boolean enabled;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder nodes(List<Node> nodes) { this.nodes = nodes; return this; }
        public Builder edges(List<Edge> edges) { this.edges = edges; return this; }
        public Builder startNodeId(String startNodeId) { this.startNodeId = startNodeId; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Workflow build() { return new Workflow(id, name, description, nodes, edges, startNodeId, enabled); }
    }
}
