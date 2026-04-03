package com.powerflow.engine.domain.port.outbound.http;

import java.util.Map;

public class HttpRequest {
    private final String url;
    private final String method;
    private final Map<String, String> headers;
    private final Object body;
    private final int timeout;

    private HttpRequest(Builder builder) {
        this.url = builder.url;
        this.method = builder.method;
        this.headers = builder.headers;
        this.body = builder.body;
        this.timeout = builder.timeout;
    }

    public String getUrl() { return url; }
    public String getMethod() { return method; }
    public Map<String, String> getHeaders() { return headers; }
    public Object getBody() { return body; }
    public int getTimeout() { return timeout; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String url;
        private String method = "GET";
        private Map<String, String> headers = Map.of();
        private Object body;
        private int timeout = 30000;

        public Builder url(String url) { this.url = url; return this; }
        public Builder method(String method) { this.method = method; return this; }
        public Builder headers(Map<String, String> headers) { this.headers = headers; return this; }
        public Builder body(Object body) { this.body = body; return this; }
        public Builder timeout(int timeout) { this.timeout = timeout; return this; }
        public HttpRequest build() { return new HttpRequest(this); }
    }
}
