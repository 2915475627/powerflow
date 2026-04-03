package com.powerflow.web.controller;

import com.powerflow.web.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatService.ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        String sessionId = request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString();
        var response = chatService.sendMessage(sessionId, request.message(), request.workflowId(), request.apiKey());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{sessionId}/history")
    public ResponseEntity<ChatService.ChatHistoryResponse> getHistory(@PathVariable String sessionId) {
        var history = chatService.getHistory(sessionId);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId) {
        chatService.clearSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    public record ChatRequest(
        String sessionId,
        String message,
        String workflowId,
        String apiKey
    ) {}
}
