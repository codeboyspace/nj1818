package com.genc.arfoms1.repository;

import com.genc.arfoms1.model.SeatInventory;
import com.genc.arfoms1.model.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatInventoryRepository extends JpaRepository<SeatInventory, Long> {

    List<SeatInventory> findByFlightId(Integer flightId);

    List<SeatInventory> findByFlightIdOrderBySeatNumberAsc(Integer flightId);

    List<SeatInventory> findByFlightIdAndSeatStatus(Integer flightId, SeatStatus seatStatus);

    Optional<SeatInventory> findByFlightIdAndSeatNumber(Integer flightId, String seatNumber);
}

