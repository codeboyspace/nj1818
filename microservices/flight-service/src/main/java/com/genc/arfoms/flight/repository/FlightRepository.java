package com.genc.arfoms.flight.repository;

import com.genc.arfoms.flight.model.Flight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FlightRepository extends JpaRepository<Flight, Long> {
	Optional<Flight> findByFlightNumber(String flightNumber);
}


