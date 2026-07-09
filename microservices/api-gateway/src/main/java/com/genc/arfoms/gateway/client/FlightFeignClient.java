package com.genc.arfoms.gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "flight-service", url = "${gateway.flight-service.base-url}")
public interface FlightFeignClient {

    @GetMapping("/api/flights")
    List<Map<String, Object>> getFlights();

    @PostMapping("/api/flights")
    Map<String, Object> createFlight(@RequestBody Map<String, Object> payload);

    @PatchMapping("/api/flights/{id}/schedule")
    Map<String, Object> updateFlightSchedule(@PathVariable("id") Long flightId, @RequestBody Map<String, Object> payload);

    @PatchMapping("/api/flights/{id}/status")
    Map<String, Object> updateFlightStatus(@PathVariable("id") Long flightId, @RequestBody Map<String, Object> payload);

    @PatchMapping("/api/flights/{id}/fare-class")
    Map<String, Object> updateFlightFares(@PathVariable("id") Long flightId, @RequestBody Map<String, Object> payload);

    @DeleteMapping("/api/flights/{id}")
    void deleteFlight(@PathVariable("id") Long flightId);
}
