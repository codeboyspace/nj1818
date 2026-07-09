package com.genc.arfoms.gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.net.URI;

@FeignClient(name = "dynamic-proxy-service", url = "http://placeholder")
public interface DynamicProxyFeignClient {

    @GetMapping
    ResponseEntity<byte[]> forwardGet(URI baseUri, @RequestHeader HttpHeaders headers);

    @PostMapping
    ResponseEntity<byte[]> forwardPost(URI baseUri, @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType, @RequestHeader HttpHeaders headers, @RequestBody Object body);

    @PutMapping
    ResponseEntity<byte[]> forwardPut(URI baseUri, @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType, @RequestHeader HttpHeaders headers, @RequestBody Object body);

    @PatchMapping
    ResponseEntity<byte[]> forwardPatch(URI baseUri, @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType, @RequestHeader HttpHeaders headers, @RequestBody Object body);

    @DeleteMapping
    ResponseEntity<byte[]> forwardDelete(URI baseUri, @RequestHeader HttpHeaders headers, @RequestBody(required = false) Object body);
}
