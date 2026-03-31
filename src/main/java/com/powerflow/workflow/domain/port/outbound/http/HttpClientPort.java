package com.powerflow.workflow.domain.port.outbound.http;

public interface HttpClientPort {
    HttpResponse request(HttpRequest request);
}
