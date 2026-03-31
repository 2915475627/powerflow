package com.powerflow.workflow.domain.port.outbound.http;

import java.util.Map;

public class HttpResponse {
    private final int statusCode;
    private final Map<String, String> headers;
    private final String body;
    private final long durationMs;

    private HttpResponse(Builder builder) {
        this.statusCode = builder.statusCode;
        this.headers = builder.headers;
        this.body = builder.body;
        this.durationMs = builder.durationMs;
    }

    public int getStatusCode() { return statusCode; }
    public Map<String, String> getHeaders() { return headers; }
    public String getBody() { return body; }
    public long getDurationMs() { return durationMs; }

    public boolean isSuccess() { return statusCode >= 200 && statusCode < 300; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private int statusCode;
        private Map<String, String> headers = Map.of();
        private String body;
        private long durationMs;

        public Builder statusCode(int statusCode) { this.statusCode = statusCode; return this; }
        public Builder headers(Map<String, String> headers) { this.headers = headers; return this; }
        public Builder body(String body) { this.body = body; return this; }
        public Builder durationMs(long durationMs) { this.durationMs = durationMs; return this; }
        public HttpResponse build() { return new HttpResponse(this); }
    }
}
