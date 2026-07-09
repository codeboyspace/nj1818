package com.genc.arfoms.gateway.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
public class GatewayProxyService {

    private final RestTemplate restTemplate;

    public GatewayProxyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<byte[]> forward(String baseUrl, String path, String query, HttpMethod method,
                                          HttpHeaders inboundHeaders, byte[] body) {
        String targetUrl = baseUrl + path + (query == null || query.isBlank() ? "" : "?" + query);

        HttpHeaders outboundHeaders = new HttpHeaders();
        outboundHeaders.putAll(inboundHeaders);
        outboundHeaders.remove(HttpHeaders.HOST);
        outboundHeaders.remove(HttpHeaders.CONTENT_LENGTH);

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(body, outboundHeaders);

        try {
            return restTemplate.exchange(targetUrl, method, requestEntity, byte[].class);
        } catch (HttpStatusCodeException ex) {
            HttpHeaders responseHeaders = ex.getResponseHeaders() == null ? new HttpHeaders() : ex.getResponseHeaders();
            return ResponseEntity.status(ex.getStatusCode()).headers(responseHeaders).body(ex.getResponseBodyAsByteArray());
        }
    }
}

