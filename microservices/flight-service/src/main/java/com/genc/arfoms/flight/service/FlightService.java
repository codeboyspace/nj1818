package com.genc.arfoms.flight.service;

import com.genc.arfoms.flight.dto.FlightDistanceResponse;
import com.genc.arfoms.flight.model.Flight;
import com.genc.arfoms.flight.model.FlightStatus;
import com.genc.arfoms.flight.repository.FlightRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FlightService {

    private final FlightRepository flightRepository;
    private final AirportDistanceService airportDistanceService;

    public FlightService(FlightRepository flightRepository, AirportDistanceService airportDistanceService) {
        this.flightRepository = flightRepository;
        this.airportDistanceService = airportDistanceService;
    }

    /**
     * Computes the distance (in miles) between the origin and destination
     * airports of the given flight using the Haversine formula.
     */
    public FlightDistanceResponse getFlightDistance(Long flightId) {
        Flight flight = getFlightDetails(flightId);
        double miles = airportDistanceService.distanceMiles(flight.getOrigin(), flight.getDestination());
        return new FlightDistanceResponse(
                flight.getFlightId(),
                flight.getFlightNumber(),
                flight.getOrigin(),
                flight.getDestination(),
                miles);
    }

    /** Computes the distance (in miles) between two airport IATA codes. */
    public double distanceBetween(String origin, String destination) {
        return airportDistanceService.distanceMiles(origin, destination);
    }

    public Flight addFlight(Flight flight) {
        validateFlightTimes(flight.getDepartureTime(), flight.getArrivalTime());
        return flightRepository.save(flight);
    }

    public Flight updateSchedule(Long flightId, LocalDateTime departure, LocalDateTime arrival) {
        validateFlightTimes(departure, arrival);
        Flight flight = getFlightDetails(flightId);
        flight.setDepartureTime(departure);
        flight.setArrivalTime(arrival);
        return flightRepository.save(flight);
    }

    public Flight setFareClass(Long flightId, BigDecimal economy, BigDecimal business, BigDecimal first) {
        Flight flight = getFlightDetails(flightId);
        flight.setEconomyFare(economy);
        flight.setBusinessFare(business);
        flight.setFirstFare(first);
        return flightRepository.save(flight);
    }

    public Flight getFlightDetails(Long flightId) {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight not found"));
    }

    public List<Flight> getAllFlights() {
        return flightRepository.findAll();
    }

    public Flight getByFlightNumber(String flightNumber) {
        return flightRepository.findByFlightNumber(flightNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight not found"));
    }

    public Flight updateStatus(Long flightId, FlightStatus status) {
        Flight flight = getFlightDetails(flightId);
        flight.setFlightStatus(status);
        return flightRepository.save(flight);
    }

    public void deleteFlight(Long flightId) {
        Flight flight = getFlightDetails(flightId);
        flightRepository.delete(flight);
    }

    private void validateFlightTimes(LocalDateTime departure, LocalDateTime arrival) {
        if (departure == null || arrival == null || !arrival.isAfter(departure)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arrival time must be after departure time");
        }
    }
}


