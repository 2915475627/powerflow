package com.powerflow.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Context {
    @JsonProperty("data")
    private final Map<String, Object> data;

    public Context() {
        this.data = new HashMap<>();
    }

    public Context(Map<String, Object> initialData) {
        this.data = new HashMap<>(initialData);
    }

    public void set(String key, Object value) {
        data.put(key, value);
    }

    public Optional<Object> get(String key) {
        return Optional.ofNullable(data.get(key));
    }

    public Map<String, Object> toMap() {
        return new HashMap<>(data);
    }
}
