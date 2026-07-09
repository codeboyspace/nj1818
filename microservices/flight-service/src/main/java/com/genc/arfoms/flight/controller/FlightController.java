package com.genc.arfoms.flight.controller;

import com.genc.arfoms.flight.model.Flight;
import com.genc.arfoms.flight.model.FlightStatus;
import com.genc.arfoms.flight.service.FlightService;
import com.genc.arfoms.flight.dto.FlightDistanceResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @PostMapping
    public Flight addFlight(@Valid @RequestBody Flight flight) {
        return flightService.addFlight(flight);
    }

    @PostMapping("/search")
    public List<Flight> searchFlights(@RequestBody FlightSearchRequest request) {
        return flightService.searchFlights(request.fromLocation(), request.toLocation(), request.departureDate());
    }

    @PatchMapping("/{flightId}/schedule")
    public Flight updateSchedule(@PathVariable Long flightId, @RequestBody UpdateScheduleRequest request) {
        return flightService.updateSchedule(flightId, request.departureTime(), request.arrivalTime());
    }

    @PatchMapping("/{flightId}/fare-class")
    public Flight setFareClass(@PathVariable Long flightId, @RequestBody SetFareClassRequest request) {
        return flightService.setFareClass(flightId, request.economyFare(), request.businessFare(), request.firstFare());
    }

    @GetMapping("/{flightId}")
    public Flight getFlightDetails(@PathVariable Long flightId) {
        return flightService.getFlightDetails(flightId);
    }

    @GetMapping
    public List<Flight> getAllFlights() {
        return flightService.getAllFlights();
    }

    @GetMapping("/{flightId}/distance")
    public FlightDistanceResponse getFlightDistance(@PathVariable Long flightId) {
        return flightService.getFlightDistance(flightId);
    }

    @GetMapping("/distance")
    public FlightDistanceResponse getDistanceBetween(@RequestParam String origin,
                                                     @RequestParam String destination) {
        double miles = flightService.distanceBetween(origin, destination);
        return new FlightDistanceResponse(null, null, origin.toUpperCase(), destination.toUpperCase(), miles);
    }

    @PatchMapping("/{flightId}/status")
    public Flight updateStatus(@PathVariable Long flightId, @RequestBody UpdateStatusRequest request) {
        return flightService.updateStatus(flightId, request.flightStatus());
    }

    @DeleteMapping("/{flightId}")
    public void deleteFlight(@PathVariable Long flightId) {
        flightService.deleteFlight(flightId);
    }

    public record UpdateScheduleRequest(LocalDateTime departureTime, LocalDateTime arrivalTime) {
    }

    public record SetFareClassRequest(BigDecimal economyFare, BigDecimal businessFare, BigDecimal firstFare) {
    }

    public record UpdateStatusRequest(FlightStatus flightStatus) {
    }

    public record FlightSearchRequest(String flightType, String fromLocation, String toLocation, java.time.LocalDate departureDate, Integer passengers) {
    }
}


