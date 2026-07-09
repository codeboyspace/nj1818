package com.genc.arfoms.gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "auth-service", url = "${gateway.auth-service.base-url:http://localhost:8180}")
public interface AuthFeignClient {

    @PostMapping("/api/auth/login")
    ResponseEntity<byte[]> login(@RequestBody Map<String, Object> credentials);

    @PostMapping("/api/auth/register")
    ResponseEntity<byte[]> register(@RequestBody Map<String, Object> user);
}
