package com.powerflow.workflow.domain.service;

import com.powerflow.workflow.domain.model.*;
import com.powerflow.workflow.domain.model.enums.ExecutionStatus;
import com.powerflow.workflow.domain.model.enums.NodeType;
import com.powerflow.workflow.domain.port.outbound.NodeExecutorCallback;
import com.powerflow.workflow.domain.port.outbound.NodeExecutorPort;
import com.powerflow.workflow.domain.service.handler.HttpRequestHandler;
import com.powerflow.workflow.domain.service.handler.LlmCallHandler;
import com.powerflow.workflow.domain.service.handler.ParallelHandler;
import com.powerflow.workflow.domain.service.handler.ForeachHandler;
import com.powerflow.workflow.domain.service.handler.SubworkflowHandler;
import com.powerflow.workflow.domain.service.handler.TryCatchHandler;
import com.powerflow.workflow.domain.service.handler.RetryHandler;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NodeExecutorService implements NodeExecutorPort, NodeExecutorCallback {

    private final ExpressionParser parser = new SpelExpressionParser();
    private static final Pattern MAP_ACCESSOR_PATTERN = Pattern.compile("(#input)\\.([a-zA-Z_][a-zA-Z0-9_]*)");

    private final HttpRequestHandler httpRequestHandler;
    private final LlmCallHandler llmCallHandler;
    private final ParallelHandler parallelHandler;
    private final ForeachHandler foreachHandler;
    private final SubworkflowHandler subworkflowHandler;
    private final TryCatchHandler tryCatchHandler;
    private final RetryHandler retryHandler;

    @Override
    public NodeExecutorCallback getCallback() {
        return this;
    }

    public NodeExecutorService(HttpRequestHandler httpRequestHandler,
                               LlmCallHandler llmCallHandler,
                               ParallelHandler parallelHandler,
                               ForeachHandler foreachHandler,
                               SubworkflowHandler subworkflowHandler,
                               TryCatchHandler tryCatchHandler,
                               RetryHandler retryHandler) {
        this.httpRequestHandler = httpRequestHandler;
        this.llmCallHandler = llmCallHandler;
        this.parallelHandler = parallelHandler;
        this.foreachHandler = foreachHandler;
        this.subworkflowHandler = subworkflowHandler;
        this.tryCatchHandler = tryCatchHandler;
        this.retryHandler = retryHandler;

        foreachHandler.setNodeExecutorCallback(this);
        subworkflowHandler.setNodeExecutorCallback(this);
        tryCatchHandler.setNodeExecutorCallback(this);
        retryHandler.setNodeExecutorCallback(this);
    }

    @Override
    public NodeResult execute(Node node, Context context) {
        return switch (node.getType()) {
            case DATA_PROCESSING -> executeDataProcessing(node, context);
            case CONDITION -> executeCondition(node, context);
            case HTTP_REQUEST -> httpRequestHandler.execute(node, context);
            case LLM_CALL -> llmCallHandler.execute(node, context);
            case PARALLEL -> parallelHandler.execute(node, context);
            case FOREACH -> foreachHandler.execute(node, context);
            case BRANCH -> executeBranch(node, context);
            case SUBWORKFLOW -> subworkflowHandler.execute(node, context);
            case TRY_CATCH -> tryCatchHandler.execute(node, context);
            case RETRY -> retryHandler.execute(node, context);
            case START -> executeStartNode(node, context);
            case END -> executeEndNode(node, context);
        };
    }

    private NodeResult executeEndNode(Node node, Context context) {
        // End node terminates the workflow - no next node
        return NodeResult.builder()
            .nodeId(node.getId())
            .status(ExecutionStatus.SUCCESS)
            .output(Map.of())
            .nextNodeId(null)
            .build();
    }

    private NodeResult executeStartNode(Node node, Context context) {
        String nextNodeId = (String) node.getConfig().get("nextNodeId");
        return NodeResult.builder()
            .nodeId(node.getId())
            .status(ExecutionStatus.SUCCESS)
            .output(Map.of())
            .nextNodeId(nextNodeId)
            .build();
    }

    private String transformMapAccess(String expression) {
        Matcher matcher = MAP_ACCESSOR_PATTERN.matcher(expression);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group(1) + "['" + matcher.group(2) + "']";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private NodeResult executeDataProcessing(Node node, Context context) {
        try {
            String outputKey = (String) node.getConfig().get("outputKey");
            String expression = (String) node.getConfig().get("expression");
            String nextNodeId = (String) node.getConfig().get("nextNodeId");

            EvaluationContext evalContext = new StandardEvaluationContext();
            evalContext.setVariable("input", context.toMap());

            String transformedExpr = transformMapAccess(expression);
            Object result = parser.parseExpression(transformedExpr).getValue(evalContext);

            Map<String, Object> output = new HashMap<>();
            output.put(outputKey, result);

            if (nextNodeId != null) {
                return NodeResult.builder()
                    .nodeId(node.getId())
                    .status(ExecutionStatus.SUCCESS)
                    .output(output)
                    .nextNodeId(nextNodeId)
                    .build();
            }

            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.SUCCESS)
                .output(output)
                .build();
        } catch (Exception e) {
            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.FAILED)
                .error(e.getMessage())
                .build();
        }
    }

    private NodeResult executeCondition(Node node, Context context) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> conditions = (List<Map<String, String>>) node.getConfig().get("conditions");
            String defaultNextNodeId = (String) node.getConfig().get("defaultNextNodeId");

            EvaluationContext evalContext = new StandardEvaluationContext();
            evalContext.setVariable("input", context.toMap());

            for (Map<String, String> condition : conditions) {
                String expression = condition.get("expression");
                String nextNodeId = condition.get("nextNodeId");

                String transformedExpr = transformMapAccess(expression);
                Boolean result = parser.parseExpression(transformedExpr).getValue(evalContext, Boolean.class);
                if (Boolean.TRUE.equals(result)) {
                    return NodeResult.builder()
                        .nodeId(node.getId())
                        .status(ExecutionStatus.SUCCESS)
                        .nextNodeId(nextNodeId)
                        .output(Map.of())
                        .build();
                }
            }

            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.SUCCESS)
                .nextNodeId(defaultNextNodeId)
                .output(Map.of())
                .build();
        } catch (Exception e) {
            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.FAILED)
                .error(e.getMessage())
                .build();
        }
    }

    @SuppressWarnings("unchecked")
    private NodeResult executeBranch(Node node, Context context) {
        try {
            List<Map<String, Object>> branches = (List<Map<String, Object>>) node.getConfig().get("branches");
            String defaultNextNodeId = (String) node.getConfig().get("defaultNextNodeId");

            EvaluationContext evalContext = new StandardEvaluationContext();
            evalContext.setVariable("input", context.toMap());

            for (Map<String, Object> branch : branches) {
                String expression = (String) branch.get("expression");
                String nextNodeId = (String) branch.get("nextNodeId");
                String name = (String) branch.getOrDefault("name", "unnamed");

                String transformedExpr = transformMapAccess(expression);
                Boolean result = parser.parseExpression(transformedExpr).getValue(evalContext, Boolean.class);
                if (Boolean.TRUE.equals(result)) {
                    return NodeResult.builder()
                        .nodeId(node.getId())
                        .status(ExecutionStatus.SUCCESS)
                        .nextNodeId(nextNodeId)
                        .output(Map.of("matchedBranch", name))
                        .build();
                }
            }

            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.SUCCESS)
                .nextNodeId(defaultNextNodeId)
                .output(Map.of("matchedBranch", "default"))
                .build();
        } catch (Exception e) {
            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.FAILED)
                .error(e.getMessage())
                .build();
        }
    }
}