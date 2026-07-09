package com.genc.arfoms.booking.repository;

import com.genc.arfoms.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}

