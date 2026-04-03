package com.powerflow.engine.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.powerflow.engine.domain.model.llm.LlmProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Context {
    @JsonProperty("data")
    private final Map<String, Object> data;

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("chatHistory")
    private final List<LlmProvider.ChatMessage> chatHistory;

    public Context() {
        this.data = new HashMap<>();
        this.chatHistory = new ArrayList<>();
    }

    public Context(Map<String, Object> initialData) {
        this.data = new HashMap<>(initialData);
        this.chatHistory = new ArrayList<>();
    }

    public Context(Map<String, Object> initialData, String sessionId) {
        this.data = new HashMap<>(initialData);
        this.sessionId = sessionId;
        this.chatHistory = new ArrayList<>();
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

    // Session management
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    // Chat history management
    public List<LlmProvider.ChatMessage> getChatHistory() {
        return new ArrayList<>(chatHistory);
    }

    public void addUserMessage(String content) {
        this.chatHistory.add(new LlmProvider.ChatMessage("user", content));
    }

    public void addAssistantMessage(String content) {
        this.chatHistory.add(new LlmProvider.ChatMessage("assistant", content));
    }

    public void addMessage(String role, String content) {
        this.chatHistory.add(new LlmProvider.ChatMessage(role, content));
    }

    public void clearHistory() {
        this.chatHistory.clear();
    }
}
