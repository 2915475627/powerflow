package com.powerflow.engine.domain.model.llm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating LLM providers.
 */
public class LlmProviderFactory {

    private static final Map<String, LlmProvider> PROVIDERS = new ConcurrentHashMap<>();

    static {
        // Register built-in providers
        register(new OpenAiLlmProvider());
        register(new DeepSeekLlmProvider());
        register(new MiniMaxLlmProvider());
    }

    /**
     * Register a provider.
     */
    public static void register(LlmProvider provider) {
        PROVIDERS.put(provider.getProviderName().toLowerCase(), provider);
    }

    /**
     * Get a provider by name.
     */
    public static LlmProvider get(String providerName) {
        LlmProvider provider = PROVIDERS.get(providerName.toLowerCase());
        if (provider == null) {
            throw new IllegalArgumentException("Unknown LLM provider: " + providerName);
        }
        return provider;
    }

    /**
     * Get a provider that supports the given model.
     */
    public static LlmProvider getForModel(String model) {
        for (LlmProvider provider : PROVIDERS.values()) {
            if (provider.supportsModel(model)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("No provider found for model: " + model);
    }

    /**
     * Get provider based on config (checks provider name first, then model).
     */
    public static LlmProvider getFromConfig(LlmConfig config) {
        String model = config.getModel();
        if (model != null) {
            // Try to find provider by model first
            for (LlmProvider provider : PROVIDERS.values()) {
                if (provider.supportsModel(model)) {
                    return provider;
                }
            }
        }
        // Fall back to provider name
        if (config.getApiKey() != null) {
            if (config.getApiKey().startsWith("sk-")) {
                return get("openai");
            }
            if (config.getApiKey().startsWith("sk-")) {
                return get("deepseek");
            }
        }
        return get("openai"); // default
    }
}
