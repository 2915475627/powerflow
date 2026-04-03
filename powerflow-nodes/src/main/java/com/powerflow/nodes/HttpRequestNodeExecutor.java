package com.powerflow.nodes;

import com.powerflow.engine.domain.enums.ExecutionStatus;
import com.powerflow.engine.domain.enums.NodeType;
import com.powerflow.engine.domain.model.Context;
import com.powerflow.engine.domain.model.Node;
import com.powerflow.engine.domain.model.NodeResult;
import com.powerflow.engine.domain.port.outbound.NodeExecutorPort;
import com.powerflow.engine.domain.port.outbound.http.HttpClientPort;
import com.powerflow.engine.domain.port.outbound.http.HttpRequest;
import com.powerflow.engine.domain.port.outbound.http.HttpResponse;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes HTTP_REQUEST nodes - makes HTTP calls to external services.
 */
public class HttpRequestNodeExecutor implements NodeExecutorPort {

    private HttpClientPort httpClient;
    private final ExpressionParser parser = new SpelExpressionParser();
    private static final Pattern MAP_ACCESSOR_PATTERN = Pattern.compile("(#input)\\.([a-zA-Z_][a-zA-Z0-9_]*)");

    public HttpRequestNodeExecutor() {
        // Default constructor for SPI loading
        this.httpClient = null;
    }

    public HttpRequestNodeExecutor(HttpClientPort httpClient) {
        this.httpClient = httpClient;
    }

    private HttpClientPort getHttpClient() {
        if (httpClient == null) {
            // Placeholder implementation for demo
            httpClient = request -> new HttpResponse(200, "{\"message\": \"placeholder response\"}", Map.of(), 0);
        }
        return httpClient;
    }

    @Override
    public NodeType supportedType() {
        return NodeType.HTTP_REQUEST;
    }

    @Override
    public NodeResult execute(Node node, Context context) {
        try {
            String url = (String) node.getConfig().get("url");
            String method = (String) node.getConfig().getOrDefault("method", "GET");
            String outputKey = (String) node.getConfig().getOrDefault("outputKey", "httpResponse");
            Object body = node.getConfig().get("body");
            Integer timeout = (Integer) node.getConfig().getOrDefault("timeout", 30000);

            @SuppressWarnings("unchecked")
            Map<String, String> headers = (Map<String, String>) node.getConfig().getOrDefault("headers", new HashMap<>());

            String resolvedUrl = resolveExpression(url, context);
            String resolvedMethod = resolveExpression(method, context);
            Object resolvedBody = resolveBody(body, context);
            Map<String, String> resolvedHeaders = resolveHeaders(headers, context);

            HttpRequest request = HttpRequest.builder()
                .url(resolvedUrl)
                .method(resolvedMethod)
                .headers(resolvedHeaders)
                .body(resolvedBody)
                .timeout(timeout)
                .build();

            HttpResponse response = getHttpClient().request(request);

            Map<String, Object> output = new HashMap<>();
            output.put(outputKey, Map.of(
                "statusCode", response.getStatusCode(),
                "body", response.getBody(),
                "headers", response.getHeaders(),
                "durationMs", response.getDurationMs(),
                "success", response.isSuccess()
            ));

            String nextNodeId = (String) node.getConfig().get("nextNodeId");

            return NodeResult.builder()
                .nodeId(node.getId())
                .status(response.isSuccess() ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED)
                .output(output)
                .nextNodeId(nextNodeId)
                .error(response.isSuccess() ? null : "HTTP request failed with status: " + response.getStatusCode())
                .build();

        } catch (Exception e) {
            return NodeResult.builder()
                .nodeId(node.getId())
                .status(ExecutionStatus.FAILED)
                .error(e.getMessage())
                .build();
        }
    }

    private String resolveExpression(String expression, Context context) {
        if (expression == null) return null;
        EvaluationContext evalContext = new StandardEvaluationContext();
        evalContext.setVariable("input", context.toMap());
        String transformed = transformMapAccess(expression);
        Object result = parser.parseExpression(transformed).getValue(evalContext);
        return result != null ? result.toString() : expression;
    }

    private Object resolveBody(Object body, Context context) {
        if (body == null) return null;
        if (body instanceof String) {
            return resolveExpression((String) body, context);
        }
        return body;
    }

    private Map<String, String> resolveHeaders(Map<String, String> headers, Context context) {
        Map<String, String> resolved = new HashMap<>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            resolved.put(entry.getKey(), resolveExpression(entry.getValue(), context));
        }
        return resolved;
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
}
