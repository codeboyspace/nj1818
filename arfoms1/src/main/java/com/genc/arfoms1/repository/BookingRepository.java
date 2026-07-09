package com.genc.arfoms1.repository;

import com.genc.arfoms1.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

	Optional<Booking> findTopByOrderByIdDesc();

	List<Booking> findAllByOrderByIdDesc();

	Optional<Booking> findByPnr(String pnr);
}
