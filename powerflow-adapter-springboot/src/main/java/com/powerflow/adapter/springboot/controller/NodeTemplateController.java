package com.powerflow.adapter.springboot.controller;

import com.powerflow.core.domain.enums.NodeType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/node-templates")
public class NodeTemplateController {

    @GetMapping
    public ResponseEntity<List<NodeTemplateResponse>> listAll() {
        List<NodeTemplateResponse> templates = Arrays.stream(NodeType.values())
            .map(this::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NodeTemplateResponse> getById(@PathVariable String id) {
        NodeType nodeType;
        try {
            nodeType = NodeType.valueOf(id.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(nodeType));
    }

    @GetMapping("/type/{nodeType}")
    public ResponseEntity<List<NodeTemplateResponse>> listByNodeType(@PathVariable String nodeType) {
        try {
            NodeType type = NodeType.valueOf(nodeType.toUpperCase());
            return ResponseEntity.ok(List.of(toResponse(type)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private NodeTemplateResponse toResponse(NodeType nodeType) {
        return new NodeTemplateResponse(
            nodeType.name().toLowerCase(),
            getNodeName(nodeType),
            nodeType.name(),
            getDefaultConfig(nodeType),
            true,
            getRemark(nodeType),
            null
        );
    }

    private String getNodeName(NodeType nodeType) {
        return switch (nodeType) {
            case DATA_PROCESSING -> "Data Processing";
            case CONDITION -> "Condition";
            case HTTP_REQUEST -> "HTTP Request";
            case LLM_CALL -> "LLM Call";
            case PARALLEL -> "Parallel";
            case FOREACH -> "Foreach";
            case BRANCH -> "Branch";
            case SUBWORKFLOW -> "Subworkflow";
            case TRY_CATCH -> "Try Catch";
            case RETRY -> "Retry";
            case START -> "Start";
            case END -> "End";
            case JOIN -> "Join";
        };
    }

    private String getRemark(NodeType nodeType) {
        return switch (nodeType) {
            case DATA_PROCESSING -> "Process and transform data";
            case CONDITION -> "Branch based on condition";
            case HTTP_REQUEST -> "Make HTTP requests";
            case LLM_CALL -> "Call LLM APIs";
            case PARALLEL -> "Execute branches in parallel";
            case FOREACH -> "Iterate over items";
            case BRANCH -> "Branch workflow";
            case SUBWORKFLOW -> "Call another workflow";
            case TRY_CATCH -> "Handle exceptions";
            case RETRY -> "Retry on failure";
            case START -> "Workflow start";
            case END -> "Workflow end";
            case JOIN -> "Join parallel branches";
        };
    }

    private Map<String, Object> getDefaultConfig(NodeType nodeType) {
        return switch (nodeType) {
            case START, END -> Map.of();
            case HTTP_REQUEST -> Map.of(
                "url", "",
                "method", "GET",
                "headers", Map.of(),
                "body", ""
            );
            case LLM_CALL -> Map.of(
                "provider", "openai",
                "model", "gpt-4",
                "prompt", ""
            );
            case CONDITION -> Map.of(
                "expression", "",
                "trueNodeId", "",
                "falseNodeId", ""
            );
            case PARALLEL -> Map.of(
                "branchNodeIds", List.of()
            );
            case FOREACH -> Map.of(
                "items", List.of(),
                "itemVariable", "item"
            );
            case BRANCH -> Map.of(
                "branches", List.of()
            );
            case SUBWORKFLOW -> Map.of(
                "workflowId", ""
            );
            case TRY_CATCH -> Map.of(
                "tryNodeId", "",
                "catchNodeId", ""
            );
            case RETRY -> Map.of(
                "maxAttempts", 3,
                "retryDelayMs", 1000
            );
            case JOIN -> Map.of(
                "requiredBranches", List.of()
            );
            case DATA_PROCESSING -> Map.of(
                "operation", "transform",
                "input", "",
                "output", ""
            );
        };
    }

    public record NodeTemplateResponse(
        String id,
        String name,
        String nodeType,
        Map<String, Object> config,
        boolean active,
        String remark,
        String website
    ) {}
}
