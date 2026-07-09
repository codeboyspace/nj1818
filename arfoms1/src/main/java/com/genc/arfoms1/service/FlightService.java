package com.genc.arfoms1.service;

import com.genc.arfoms1.model.Flight;
import com.genc.arfoms1.model.enums.FlightStatus;

import java.util.List;
import java.util.Optional;

public interface FlightService {

    List<Flight> getAllFlights();

    Optional<Flight> getFlightDetails(String flightNumber);

    Flight getFlight(Long flightId);

    void addFlight(Flight flight);

    void deleteFlight(String flightNumber);

    void updateSchedule(String flightNumber, String departureTime, String arrivalTime, FlightStatus flightStatus);

    void setFares(String flightNumber, double economyFare, double premiumFare, double firstFare);

    void setFareClass(String flightNumber, double fare, String fareClass);
}
