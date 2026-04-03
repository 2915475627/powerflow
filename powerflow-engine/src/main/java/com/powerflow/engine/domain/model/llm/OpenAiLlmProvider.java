package com.powerflow.engine.domain.model.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAI LLM provider implementation.
 */
public class OpenAiLlmProvider implements LlmProvider {

    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private final HttpClient httpClient;

    public OpenAiLlmProvider() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }

    @Override
    public String generate(String prompt, LlmConfig config) {
        return generateWithHistory(prompt, List.of(), config);
    }

    @Override
    public String generateWithHistory(String prompt, List<ChatMessage> history, LlmConfig config) {
        try {
            String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : DEFAULT_BASE_URL;
            String model = config.getModel() != null ? config.getModel() : "gpt-4";
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
                throw new RuntimeException("OpenAI API error: " + response.statusCode() + " - " + response.body());
            }

            // Parse response - simple JSON parsing
            String body = response.body();
            // Extract "content" from choices[0].message.content
            int contentStart = body.indexOf("\"content\":");
            if (contentStart == -1) {
                throw new RuntimeException("Unexpected response format: " + body);
            }
            int contentQuoteStart = body.indexOf("\"", contentStart + 10);
            int contentQuoteEnd = body.indexOf("\"", contentQuoteStart + 1);
            return body.substring(contentQuoteStart + 1, contentQuoteEnd);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("OpenAI API call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public boolean supportsModel(String model) {
        return model != null && (model.startsWith("gpt-") || model.startsWith("o1-"));
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
