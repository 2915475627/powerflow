package com.powerflow.engine.domain.model.llm;

import java.util.List;
import java.util.Map;

/**
 * Interface for LLM providers.
 * Implementations: OpenAI, DeepSeek, Zhipu, etc.
 */
public interface LlmProvider {

    /**
     * Generate a response from the LLM.
     *
     * @param prompt The user prompt
     * @param config LLM configuration
     * @return LLM response text
     */
    String generate(String prompt, LlmConfig config);

    /**
     * Generate a response with conversation history.
     *
     * @param prompt    The user prompt
     * @param history   Previous messages (role/content pairs)
     * @param config    LLM configuration
     * @return LLM response text
     */
    String generateWithHistory(String prompt, List<ChatMessage> history, LlmConfig config);

    /**
     * Provider name identifier (e.g., "openai", "deepseek").
     */
    String getProviderName();

    /**
     * Check if this provider supports the given model prefix.
     */
    boolean supportsModel(String model);

    /**
     * Chat message for conversation history.
     */
    record ChatMessage(String role, String content) {}
}
