package com.genc.arfoms.gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "booking-service", url = "${gateway.booking-service.base-url}")
public interface BookingFeignClient {

    @PostMapping("/api/bookings")
    Map<String, Object> createBooking(@RequestBody Map<String, Object> payload);

    @GetMapping("/api/bookings")
    List<Map<String, Object>> getBookings();

    @GetMapping("/api/bookings/{id}")
    Map<String, Object> getBooking(@PathVariable("id") Long bookingId);

    @PatchMapping("/api/bookings/{id}")
    Map<String, Object> updateBooking(@PathVariable("id") Long bookingId, @RequestBody Map<String, Object> payload);

    @PatchMapping("/api/bookings/{id}/cancel")
    Map<String, Object> cancelBooking(@PathVariable("id") Long bookingId);
}
