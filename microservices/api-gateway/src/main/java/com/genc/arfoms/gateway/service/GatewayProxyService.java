package com.genc.arfoms.gateway.service;

import com.genc.arfoms.gateway.client.DynamicProxyFeignClient;
import feign.FeignException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Optional;

@Service
public class GatewayProxyService {

    private final DynamicProxyFeignClient feignClient;

    public GatewayProxyService(DynamicProxyFeignClient feignClient) {
        this.feignClient = feignClient;
    }

    public ResponseEntity<byte[]> forward(String baseUrl, String path, String query, HttpMethod method,
                                          HttpHeaders inboundHeaders, Object body) {
        String targetUrl = baseUrl + path + (query == null || query.isBlank() ? "" : "?" + query);
        URI uri = URI.create(targetUrl);

        HttpHeaders outboundHeaders = new HttpHeaders();
        outboundHeaders.putAll(inboundHeaders);
        outboundHeaders.remove(HttpHeaders.HOST);
        outboundHeaders.remove(HttpHeaders.CONTENT_LENGTH);

        try {
            String contentType = outboundHeaders.getFirst(HttpHeaders.CONTENT_TYPE);
            return switch (method.name()) {
                case "GET" -> feignClient.forwardGet(uri, outboundHeaders);
                case "POST" -> feignClient.forwardPost(uri, contentType, outboundHeaders, body);
                case "PUT" -> feignClient.forwardPut(uri, contentType, outboundHeaders, body);
                case "PATCH" -> feignClient.forwardPatch(uri, contentType, outboundHeaders, body);
                case "DELETE" -> feignClient.forwardDelete(uri, outboundHeaders, body);
                default -> ResponseEntity.status(405).build();
            };
        } catch (FeignException ex) {
            HttpHeaders responseHeaders = new HttpHeaders();
            if (ex.responseHeaders() != null) {
                ex.responseHeaders().forEach((k, v) -> responseHeaders.addAll(k, v.stream().toList()));
            }
            byte[] responseBody = ex.content() != null ? ex.content() : new byte[0];
            return ResponseEntity.status(ex.status()).headers(responseHeaders).body(responseBody);
        }
    }
}
