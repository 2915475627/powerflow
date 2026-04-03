package com.powerflow.engine.domain.port.outbound.http;

import java.util.Map;

public class HttpResponse {
    private final int statusCode;
    private final Object body;
    private final Map<String, String> headers;
    private final long durationMs;
    private final boolean success;

    public HttpResponse(int statusCode, Object body, Map<String, String> headers, long durationMs) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
        this.durationMs = durationMs;
        this.success = statusCode >= 200 && statusCode < 300;
    }

    public int getStatusCode() { return statusCode; }
    public Object getBody() { return body; }
    public Map<String, String> getHeaders() { return headers; }
    public long getDurationMs() { return durationMs; }
    public boolean isSuccess() { return success; }
}
