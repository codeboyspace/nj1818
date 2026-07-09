package com.genc.arfoms.gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "crew-service", url = "${gateway.crew-service.base-url}")
public interface CrewFeignClient {

    @GetMapping("/api/crew/roster")
    List<Map<String, Object>> getCrewRoster();

    @PatchMapping("/api/crew/{id}/swap")
    Map<String, Object> swapCrew(@PathVariable("id") Long assignmentId, @RequestBody Map<String, Object> payload);
}
