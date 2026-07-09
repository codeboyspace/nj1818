package com.genc.arfoms.gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "loyalty-service", url = "${gateway.loyalty-service.base-url}")
public interface LoyaltyFeignClient {

    @GetMapping("/api/loyalty")
    List<Map<String, Object>> getLoyaltyMembers();

    @GetMapping("/api/loyalty/{id}")
    Map<String, Object> getLoyaltyMember(@PathVariable("id") Long memberId);

    @PatchMapping("/api/loyalty/{id}/credit")
    Map<String, Object> creditLoyalty(@PathVariable("id") Long memberId, @RequestBody Map<String, Object> payload);

    @PatchMapping("/api/loyalty/{id}/redeem")
    Map<String, Object> redeemLoyalty(@PathVariable("id") Long memberId, @RequestBody Map<String, Object> payload);
}
