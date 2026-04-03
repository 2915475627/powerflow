package com.powerflow.engine.domain.port.outbound.http;

/**
 * Outbound port for HTTP client operations.
 */
public interface HttpClientPort {
    HttpResponse request(HttpRequest request);
}
