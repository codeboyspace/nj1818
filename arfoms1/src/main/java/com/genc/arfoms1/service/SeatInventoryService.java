package com.genc.arfoms1.service;

import com.genc.arfoms1.model.SeatInventory;
import com.genc.arfoms1.model.enums.SeatStatus;
import com.genc.arfoms1.repository.SeatInventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SeatInventoryService {

    private final SeatInventoryRepository seatInventoryRepository;

    @Autowired
    public SeatInventoryService(SeatInventoryRepository seatInventoryRepository) {
        this.seatInventoryRepository = seatInventoryRepository;
    }

    public List<SeatInventory> getSeatsByFlight(Long flightId) {
        return seatInventoryRepository.findByFlightId(toInt(flightId));
    }

    public List<SeatInventory> getAvailableSeats(Long flightId) {
        return seatInventoryRepository.findByFlightIdAndSeatStatus(toInt(flightId), SeatStatus.AVAILABLE);
    }

    public List<SeatInventory> getBookedSeats(Long flightId) {
        return seatInventoryRepository.findByFlightIdAndSeatStatus(toInt(flightId), SeatStatus.BOOKED);
    }


    public long countAvailableSeats(Long flightId) {
        return seatInventoryRepository.findByFlightIdAndSeatStatus(toInt(flightId), SeatStatus.AVAILABLE).size();
    }

    public long countBookedSeats(Long flightId) {
        return seatInventoryRepository.findByFlightIdAndSeatStatus(toInt(flightId), SeatStatus.BOOKED).size();
    }

    @Transactional
    public boolean confirmSeatSelection(Long flightId, String seatNumber) {
        Optional<SeatInventory> seatOpt = seatInventoryRepository.findByFlightIdAndSeatNumber(toInt(flightId), seatNumber);
        if (seatOpt.isPresent()) {
            SeatInventory seat = seatOpt.get();
            if (seat.getSeatStatus() == SeatStatus.AVAILABLE) {
                seat.setSeatStatus(SeatStatus.BOOKED);
                seatInventoryRepository.save(seat);
                return true;
            }
        }
        return false;
    }

    @Transactional
    public void resetSeatSelection(Long flightId, String seatNumber) {
        Optional<SeatInventory> seatOpt = seatInventoryRepository.findByFlightIdAndSeatNumber(toInt(flightId), seatNumber);
        if (seatOpt.isPresent()) {
            SeatInventory seat = seatOpt.get();
            seat.setSeatStatus(SeatStatus.AVAILABLE);
            seatInventoryRepository.save(seat);
        }
    }

    private Integer toInt(Long value) {
        return value == null ? null : value.intValue();
    }
}

