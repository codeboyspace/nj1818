package com.genc.arfoms.gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@FeignClient(name = "checkin-service", url = "${gateway.checkin-service.base-url}")
public interface CheckinFeignClient {

    @GetMapping("/api/checkin")
    List<Map<String, Object>> getCheckins();
}
