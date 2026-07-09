package com.genc.arfoms1.controller;

import com.genc.arfoms1.dto.FareUpdateRequest;
import com.genc.arfoms1.dto.FlightRequest;
import com.genc.arfoms1.dto.ScheduleUpdateRequest;
import com.genc.arfoms1.exception.FlightException;
import com.genc.arfoms1.model.Flight;
import com.genc.arfoms1.model.InternationalAirports;
import com.genc.arfoms1.model.enums.FlightStatus;
import com.genc.arfoms1.model.IndianAirports;
import com.genc.arfoms1.service.FlightService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/v1/flights")
@CrossOrigin(origins = "*")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @GetMapping
    public List<Flight> getAllFlights() {
        return flightService.getAllFlights();
    }

    @GetMapping("/{flightNumber}")
    public Flight getFlightByNumber(@PathVariable String flightNumber) {
        return flightService.getFlightDetails(flightNumber)
                .orElseThrow(() -> new FlightException(HttpStatus.NOT_FOUND, "Flight not found for number " + flightNumber));
    }

    @PostMapping
    public ResponseEntity<Flight> addFlight(@RequestBody FlightRequest request) {
        Flight flight = request.toEntity();
        flightService.addFlight(flight);
        return ResponseEntity.status(HttpStatus.CREATED).body(getFlightByNumber(flight.getFlightNumber()));
    }

    @PutMapping("/{flightNumber}/schedule")
    public Flight updateSchedule(@PathVariable String flightNumber,
                                 @RequestBody ScheduleUpdateRequest request) {
        flightService.updateSchedule(flightNumber, request.getDepartureTime(), request.getArrivalTime(), request.getFlightStatus());
        return getFlightByNumber(flightNumber);
    }

    @PutMapping("/{flightNumber}/fares")
    public Flight setFares(@PathVariable String flightNumber,
                           @RequestBody FareUpdateRequest request) {
        flightService.setFares(flightNumber, request.getEconomyFare(), request.getPremiumFare(), request.getFirstFare());
        return getFlightByNumber(flightNumber);
    }

    @DeleteMapping("/{flightNumber}")
    public ResponseEntity<Void> deleteFlight(@PathVariable String flightNumber) {
        flightService.deleteFlight(flightNumber);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/metadata")
    public Map<String, Object> getMetadata() {
        List<IndianAirports.Airport> indianAirports = IndianAirports.AIRPORTS;
        List<IndianAirports.Airport> internationalAirports = InternationalAirports.AIRPORTS;
        List<IndianAirports.Airport> allAirports = Stream.concat(indianAirports.stream(), internationalAirports.stream())
                .toList();

        return Map.of(
                "statuses", FlightStatus.values(),
                "airports", allAirports,
                "indianAirports", indianAirports,
                "internationalAirports", internationalAirports
        );
    }
}

