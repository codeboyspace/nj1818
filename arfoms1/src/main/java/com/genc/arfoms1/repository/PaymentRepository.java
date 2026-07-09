package com.genc.arfoms1.repository;

import com.genc.arfoms1.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Optional<Payment> findByFlightIdAndSeatNumber(Integer flightId, String seatNumber);
}

