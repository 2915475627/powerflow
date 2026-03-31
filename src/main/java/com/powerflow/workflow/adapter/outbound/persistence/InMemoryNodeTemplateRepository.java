package com.powerflow.workflow.adapter.outbound.persistence;

import com.powerflow.workflow.domain.model.NodeTemplate;
import com.powerflow.workflow.domain.model.enums.NodeType;
import com.powerflow.workflow.domain.port.outbound.NodeTemplateRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryNodeTemplateRepository implements NodeTemplateRepository {

    private final Map<String, NodeTemplate> store = new ConcurrentHashMap<>();

    public InMemoryNodeTemplateRepository() {
        // Initialize with default LLM templates
        initLlmTemplates();
    }

    private void initLlmTemplates() {
        // OpenAI GPT-4
        store.put("openai-gpt4", NodeTemplate.builder()
            .id("openai-gpt4")
            .name("OpenAI GPT-4")
            .nodeType(NodeType.LLM_CALL)
            .config(Map.of(
                "provider", "openai",
                "model", "gpt-4",
                "temperature", 0.7,
                "maxTokens", 2000
            ))
            .active(true)
            .build());

        // OpenAI GPT-4 Turbo
        store.put("openai-gpt4-turbo", NodeTemplate.builder()
            .id("openai-gpt4-turbo")
            .name("OpenAI GPT-4 Turbo")
            .nodeType(NodeType.LLM_CALL)
            .config(Map.of(
                "provider", "openai",
                "model", "gpt-4-turbo",
                "temperature", 0.7,
                "maxTokens", 4000
            ))
            .active(true)
            .build());

        // OpenAI GPT-3.5 Turbo
        store.put("openai-gpt35", NodeTemplate.builder()
            .id("openai-gpt35")
            .name("OpenAI GPT-3.5 Turbo")
            .nodeType(NodeType.LLM_CALL)
            .config(Map.of(
                "provider", "openai",
                "model", "gpt-3.5-turbo",
                "temperature", 0.7,
                "maxTokens", 2000
            ))
            .active(true)
            .build());

        // Anthropic Claude 3 Opus
        store.put("anthropic-claude3-opus", NodeTemplate.builder()
            .id("anthropic-claude3-opus")
            .name("Anthropic Claude 3 Opus")
            .nodeType(NodeType.LLM_CALL)
            .config(Map.of(
                "provider", "anthropic",
                "model", "claude-3-opus-20240229",
                "temperature", 0.7,
                "maxTokens", 4000
            ))
            .active(true)
            .build());

        // Anthropic Claude 3 Sonnet
        store.put("anthropic-claude3-sonnet", NodeTemplate.builder()
            .id("anthropic-claude3-sonnet")
            .name("Anthropic Claude 3 Sonnet")
            .nodeType(NodeType.LLM_CALL)
            .config(Map.of(
                "provider", "anthropic",
                "model", "claude-3-sonnet-20240229",
                "temperature", 0.7,
                "maxTokens", 4000
            ))
            .active(true)
            .build());

        // Anthropic Claude 3 Haiku
        store.put("anthropic-claude3-haiku", NodeTemplate.builder()
            .id("anthropic-claude3-haiku")
            .name("Anthropic Claude 3 Haiku")
            .nodeType(NodeType.LLM_CALL)
            .config(Map.of(
                "provider", "anthropic",
                "model", "claude-3-haiku-20240307",
                "temperature", 0.7,
                "maxTokens", 4000
            ))
            .active(true)
            .build());

        // Azure OpenAI GPT-4
        store.put("azure-gpt4", NodeTemplate.builder()
            .id("azure-gpt4")
            .name("Azure OpenAI GPT-4")
            .nodeType(NodeType.LLM_CALL)
            .config(Map.of(
                "provider", "azure",
                "model", "gpt-4",
                "temperature", 0.7,
                "maxTokens", 2000,
                "apiVersion", "2024-02-01"
            ))
            .active(true)
            .build());

        // HTTP Request Templates
        store.put("http-get-json", NodeTemplate.builder()
            .id("http-get-json")
            .name("GET JSON API")
            .nodeType(NodeType.HTTP_REQUEST)
            .config(Map.of(
                "method", "GET",
                "headers", Map.of("Content-Type", "application/json"),
                "timeout", 30000,
                "outputKey", "httpResponse"
            ))
            .active(true)
            .build());

        store.put("http-post-json", NodeTemplate.builder()
            .id("http-post-json")
            .name("POST JSON API")
            .nodeType(NodeType.HTTP_REQUEST)
            .config(Map.of(
                "method", "POST",
                "headers", Map.of("Content-Type", "application/json"),
                "timeout", 30000,
                "outputKey", "httpResponse"
            ))
            .active(true)
            .build());
    }

    @Override
    public Optional<NodeTemplate> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<NodeTemplate> findByNodeType(NodeType nodeType) {
        return store.values().stream()
            .filter(t -> t.getNodeType() == nodeType)
            .collect(Collectors.toList());
    }

    @Override
    public List<NodeTemplate> findAll() {
        return store.values().stream().collect(Collectors.toList());
    }

    @Override
    public List<NodeTemplate> findActiveByNodeType(NodeType nodeType) {
        return store.values().stream()
            .filter(t -> t.getNodeType() == nodeType && t.isActive())
            .collect(Collectors.toList());
    }

    @Override
    public NodeTemplate save(NodeTemplate template) {
        store.put(template.getId(), template);
        return template;
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }
}
