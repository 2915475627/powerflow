package com.powerflow.workflow.adapter.outbound.http;

import com.powerflow.workflow.domain.port.outbound.http.HttpClientPort;
import com.powerflow.workflow.domain.port.outbound.http.HttpRequest;
import com.powerflow.workflow.domain.port.outbound.http.HttpResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestTemplateHttpClientAdapter implements HttpClientPort {

    private final RestTemplate restTemplate;

    public RestTemplateHttpClientAdapter() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public HttpResponse request(HttpRequest request) {
        long startTime = System.currentTimeMillis();

        HttpHeaders headers = new HttpHeaders();
        request.getHeaders().forEach(headers::add);

        HttpEntity<Object> entity = new HttpEntity<>(request.getBody(), headers);

        ResponseEntity<String> response = restTemplate.exchange(
            request.getUrl(),
            HttpMethod.valueOf(request.getMethod().toUpperCase()),
            entity,
            String.class
        );

        long duration = System.currentTimeMillis() - startTime;

        return HttpResponse.builder()
            .statusCode(response.getStatusCode().value())
            .headers(response.getHeaders().toSingleValueMap())
            .body(response.getBody())
            .durationMs(duration)
            .build();
    }
}
