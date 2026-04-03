package com.powerflow.web.service;

import com.powerflow.engine.domain.model.Context;
import com.powerflow.engine.domain.model.Workflow;
import com.powerflow.engine.domain.model.WorkflowExecutionResult;
import com.powerflow.engine.domain.model.llm.LlmProvider;
import com.powerflow.engine.domain.port.inbound.WorkflowEnginePort;
import com.powerflow.engine.domain.port.outbound.WorkflowRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for chat functionality - manages sessions and orchestrates workflow execution.
 */
@Service
public class ChatService {

    private final WorkflowEnginePort workflowEngine;
    private final WorkflowRepositoryPort workflowRepository;
    private final Map<String, ChatSession> sessions = new HashMap<>();

    public ChatService(WorkflowEnginePort workflowEngine, WorkflowRepositoryPort workflowRepository) {
        this.workflowEngine = workflowEngine;
        this.workflowRepository = workflowRepository;
    }

    /**
     * Send a chat message and optionally execute a workflow.
     */
    public ChatResponse sendMessage(String sessionId, String message, String workflowId, String apiKey) {
        // Get or create session
        ChatSession session = sessions.computeIfAbsent(sessionId, k -> new ChatSession(sessionId));
        session.addUserMessage(message);

        // If no workflow specified, just echo back (simple chat mode)
        if (workflowId == null || workflowId.isEmpty()) {
            String echo = "Received: " + message + " (no workflow specified)";
            session.addAssistantMessage(echo);
            return new ChatResponse(sessionId, echo, null, null, null);
        }

        // Find workflow
        Optional<Workflow> workflowOpt = workflowRepository.findById(workflowId);
        if (workflowOpt.isEmpty()) {
            String error = "Workflow not found: " + workflowId;
            return new ChatResponse(sessionId, error, null, null, null);
        }

        try {
            // Build context with user message and chat history
            Map<String, Object> initialData = new HashMap<>();
            initialData.put("input", message);
            Context context = new Context(initialData, sessionId);
            for (LlmProvider.ChatMessage msg : session.getHistory()) {
                context.addMessage(msg.role(), msg.content());
            }

            // Execute workflow
            WorkflowExecutionResult result = workflowEngine.execute(workflowId, context);

            // Extract LLM response from result
            String llmResponse = extractLlmResponse(result);
            if (llmResponse != null) {
                session.addAssistantMessage(llmResponse);
            }

            return new ChatResponse(
                sessionId,
                llmResponse,
                result.getExecutionId(),
                result.getStatus().name(),
                buildChain(result)
            );

        } catch (Exception e) {
            String error = "Execution error: " + e.getMessage();
            return new ChatResponse(sessionId, error, null, "FAILED", null);
        }
    }

    /**
     * Get chat history for a session.
     */
    public ChatHistoryResponse getHistory(String sessionId) {
        ChatSession session = sessions.get(sessionId);
        if (session == null) {
            return new ChatHistoryResponse(sessionId, List.of());
        }
        List<LlmProvider.ChatMessage> history = session.getHistory();
        List<Map<String, Object>> messageMaps = history.stream()
            .map(msg -> Map.<String, Object>of("role", msg.role(), "content", msg.content()))
            .toList();
        return new ChatHistoryResponse(sessionId, messageMaps);
    }

    /**
     * Clear a chat session.
     */
    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
    }

    private String extractLlmResponse(WorkflowExecutionResult result) {
        if (result.getFinalContext() == null) return null;
        Map<String, Object> ctx = result.getFinalContext().toMap();
        for (Object value : ctx.values()) {
            if (value instanceof String && ((String) value).length() > 10) {
                return (String) value;
            }
        }
        return ctx.toString();
    }

    private List<Map<String, Object>> buildChain(WorkflowExecutionResult result) {
        // Build chain from execution result - placeholder for now
        return List.of();
    }

    // Internal classes
    public record ChatResponse(
        String sessionId,
        String message,
        String workflowExecutionId,
        String status,
        List<Map<String, Object>> chain
    ) {}

    public record ChatHistoryResponse(
        String sessionId,
        List<Map<String, Object>> messages
    ) {}

    private static class ChatSession {
        private final String sessionId;
        private final List<LlmProvider.ChatMessage> history = new ArrayList<>();

        ChatSession(String sessionId) {
            this.sessionId = sessionId;
        }

        void addUserMessage(String content) {
            history.add(new LlmProvider.ChatMessage("user", content));
        }

        void addAssistantMessage(String content) {
            history.add(new LlmProvider.ChatMessage("assistant", content));
        }

        List<LlmProvider.ChatMessage> getHistory() {
            return new ArrayList<>(history);
        }
    }
}
