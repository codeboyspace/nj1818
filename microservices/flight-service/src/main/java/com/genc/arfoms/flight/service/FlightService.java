package com.genc.arfoms.flight.service;

import com.genc.arfoms.flight.dto.FlightDistanceResponse;
import com.genc.arfoms.flight.model.Flight;
import com.genc.arfoms.flight.model.FlightStatus;
import com.genc.arfoms.flight.repository.FlightRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.genc.arfoms.flight.exception.NoDataFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FlightService {

    private final Logger logger = LoggerFactory.getLogger(FlightService.class);
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
        logger.info("Computing distance for Flight ID: {}", flightId);
        Flight flight = getFlightDetails(flightId);
        double miles = airportDistanceService.distanceMiles(flight.getOrigin(), flight.getDestination());
        logger.info("Computed distance for Flight ID {}: {} miles", flightId, miles);
        return new FlightDistanceResponse(
                flight.getFlightId(),
                flight.getFlightNumber(),
                flight.getOrigin(),
                flight.getDestination(),
                miles);
    }

    /** Computes the distance (in miles) between two airport IATA codes. */
    public double distanceBetween(String origin, String destination) {
        logger.info("Computing distance between origin: '{}' and destination: '{}'", origin, destination);
        double miles = airportDistanceService.distanceMiles(origin, destination);
        logger.info("Distance between '{}' and '{}' is: {} miles", origin, destination, miles);
        return miles;
    }

    public Flight addFlight(Flight flight) {
        logger.info("Adding new flight: {}", flight);
        validateFlightTimes(flight.getDepartureTime(), flight.getArrivalTime());
        Flight savedFlight = flightRepository.save(flight);
        logger.info("Flight successfully added with ID: {}", savedFlight.getFlightId());
        return savedFlight;
    }

    public Flight updateSchedule(Long flightId, LocalDateTime departure, LocalDateTime arrival) {
        logger.info("Updating schedule for Flight ID: {} to departure: {}, arrival: {}", flightId, departure, arrival);
        validateFlightTimes(departure, arrival);
        Flight flight = getFlightDetails(flightId);
        flight.setDepartureTime(departure);
        flight.setArrivalTime(arrival);
        Flight updatedFlight = flightRepository.save(flight);
        logger.info("Flight ID: {} schedule updated successfully.", flightId);
        return updatedFlight;
    }

    public Flight setFareClass(Long flightId, BigDecimal economy, BigDecimal business, BigDecimal first) {
        logger.info("Setting fare classes for Flight ID: {} (Economy: {}, Business: {}, First: {})", flightId, economy, business, first);
        Flight flight = getFlightDetails(flightId);
        flight.setEconomyFare(economy);
        flight.setBusinessFare(business);
        flight.setFirstFare(first);
        Flight updatedFlight = flightRepository.save(flight);
        logger.info("Fares set successfully for Flight ID: {}", flightId);
        return updatedFlight;
    }

    public Flight getFlightDetails(Long flightId) {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> {
                    logger.warn("Flight details lookup failed: Flight ID {} not found", flightId);
                    return new NoDataFoundException("Flight not found");
                });
    }

    public List<Flight> getAllFlights() {
        logger.info("Fetching all flights from database.");
        return flightRepository.findAll();
    }

    public List<Flight> searchFlights(String origin, String destination, java.time.LocalDate departureDate) {
        logger.info("Searching flights from '{}' to '{}' on date '{}'", origin, destination, departureDate);
        List<Flight> flights = flightRepository.findByOriginAndDestination(origin, destination);
        if (departureDate != null) {
            List<Flight> filtered = flights.stream()
                    .filter(f -> f.getDepartureTime() != null && f.getDepartureTime().toLocalDate().equals(departureDate))
                    .collect(java.util.stream.Collectors.toList());
            logger.info("Search returned {} flights after date filtering.", filtered.size());
            return filtered;
        }
        logger.info("Search returned {} flights.", flights.size());
        return flights;
    }

    public Flight getByFlightNumber(String flightNumber) {
        return flightRepository.findByFlightNumber(flightNumber)
                .orElseThrow(() -> {
                    logger.warn("Flight lookup failed: Flight Number {} not found", flightNumber);
                    return new NoDataFoundException("Flight not found");
                });
    }

    public Flight updateStatus(Long flightId, FlightStatus status) {
        logger.info("Updating status of Flight ID: {} to {}", flightId, status);
        Flight flight = getFlightDetails(flightId);
        flight.setFlightStatus(status);
        Flight updatedFlight = flightRepository.save(flight);
        logger.info("Flight ID: {} status updated to {}", flightId, status);
        return updatedFlight;
    }

    public void deleteFlight(Long flightId) {
        logger.info("Deleting flight ID: {}", flightId);
        Flight flight = getFlightDetails(flightId);
        flightRepository.delete(flight);
        logger.info("Flight ID: {} successfully deleted.", flightId);
    }

    private void validateFlightTimes(LocalDateTime departure, LocalDateTime arrival) {
        if (departure == null || arrival == null || !arrival.isAfter(departure)) {
            logger.warn("Flight validation failed: invalid times (departure: {}, arrival: {})", departure, arrival);
            throw new IllegalArgumentException("Arrival time must be after departure time");
        }
    }
}


