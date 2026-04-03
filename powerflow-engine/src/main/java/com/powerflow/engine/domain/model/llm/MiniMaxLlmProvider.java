package com.powerflow.engine.domain.model.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * MiniMax LLM provider implementation.
 * MiniMax API is compatible with OpenAI's chat completions format.
 */
public class MiniMaxLlmProvider implements LlmProvider {

    private static final String DEFAULT_BASE_URL = "https://api.minimax.chat/v1";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MiniMaxLlmProvider() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String generate(String prompt, LlmConfig config) {
        return generateWithHistory(prompt, List.of(), config);
    }

    @Override
    public String generateWithHistory(String prompt, List<ChatMessage> history, LlmConfig config) {
        try {
            String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : DEFAULT_BASE_URL;
            String model = config.getModel() != null ? config.getModel() : "MiniMax-M2.7";
            double temperature = config.getTemperature();
            int maxTokens = config.getMaxTokens() > 0 ? config.getMaxTokens() : 2048;

            // Build messages array
            StringBuilder messagesJson = new StringBuilder("[");
            if (config.getSystemPrompt() != null && !config.getSystemPrompt().isEmpty()) {
                messagesJson.append(String.format(
                    "{\"role\":\"system\",\"content\":\"%s\"},",
                    escapeJson(config.getSystemPrompt())
                ));
            }
            for (ChatMessage msg : history) {
                messagesJson.append(String.format(
                    "{\"role\":\"%s\",\"content\":\"%s\"},",
                    escapeJson(msg.role()), escapeJson(msg.content())
                ));
            }
            messagesJson.append(String.format(
                "{\"role\":\"user\",\"content\":\"%s\"}]",
                escapeJson(prompt)
            ));

            String requestBody = String.format("""
                {
                    "model": "%s",
                    "messages": %s,
                    "temperature": %f,
                    "max_tokens": %d
                }
                """, model, messagesJson, temperature, maxTokens);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(120))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("MiniMax API error: " + response.statusCode() + " - " + response.body());
            }

            // Parse response using Jackson
            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode choicesNode = rootNode.path("choices");
            if (choicesNode.isArray() && choicesNode.size() > 0) {
                JsonNode messageNode = choicesNode.get(0).path("message");
                String content = messageNode.path("content").asText();
                if (content != null && !content.isEmpty()) {
                    return content.trim();
                }
            }
            throw new RuntimeException("No content in MiniMax response: " + response.body());

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("MiniMax API call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "minimax";
    }

    @Override
    public boolean supportsModel(String model) {
        return model != null && (model.toLowerCase().contains("minimax") || "MiniMax-M2.7".equals(model));
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
