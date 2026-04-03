package com.powerflow.engine.domain.model.llm;

import lombok.Builder;
import lombok.Data;

/**
 * Configuration for LLM calls.
 */
@Data
@Builder
public class LlmConfig {
    private String model;
    private double temperature;
    private int maxTokens;
    private String systemPrompt;
    private String apiKey;
    private String baseUrl;
}
