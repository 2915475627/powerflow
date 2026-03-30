package com.powerflow.workflow.domain.model;

import com.powerflow.workflow.domain.model.enums.NodeType;
import java.util.HashMap;
import java.util.Map;

public class Node {
    private final String id;
    private final String name;
    private final NodeType type;
    private final Map<String, Object> config;
    private final Map<String, String> inputMapping;
    private final Map<String, String> outputMapping;

    public Node(String id, String name, NodeType type,
                Map<String, Object> config,
                Map<String, String> inputMapping,
                Map<String, String> outputMapping) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.config = new HashMap<>(config);
        this.inputMapping = new HashMap<>(inputMapping);
        this.outputMapping = new HashMap<>(outputMapping);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public NodeType getType() { return type; }
    public Map<String, Object> getConfig() { return new HashMap<>(config); }
    public Map<String, String> getInputMapping() { return new HashMap<>(inputMapping); }
    public Map<String, String> getOutputMapping() { return new HashMap<>(outputMapping); }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String name;
        private NodeType type;
        private Map<String, Object> config = new HashMap<>();
        private Map<String, String> inputMapping = new HashMap<>();
        private Map<String, String> outputMapping = new HashMap<>();

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder type(NodeType type) { this.type = type; return this; }
        public Builder config(Map<String, Object> config) { this.config = config; return this; }
        public Builder inputMapping(Map<String, String> inputMapping) { this.inputMapping = inputMapping; return this; }
        public Builder outputMapping(Map<String, String> outputMapping) { this.outputMapping = outputMapping; return this; }
        public Node build() { return new Node(id, name, type, config, inputMapping, outputMapping); }
    }
}
