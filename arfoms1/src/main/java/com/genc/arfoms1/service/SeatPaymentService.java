package com.genc.arfoms1.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.genc.arfoms1.dto.BookingResult;
import com.genc.arfoms1.model.Flight;
import com.genc.arfoms1.model.Passenger;
import com.genc.arfoms1.model.Payment;
import com.genc.arfoms1.model.SeatInventory;
import com.genc.arfoms1.model.enums.SeatStatus;
import com.genc.arfoms1.repository.PaymentRepository;
import com.genc.arfoms1.repository.SeatInventoryRepository;

/**
 * Business logic for the seat-selection &amp; payment module.
 * <p>
 * Combines what used to be two separate services
 * ({@code SeatInventoryService} + {@code PaymentService}) into one, since they
 * back a single end-to-end flow: select seat → view fare → confirm booking.
 */
@Service
public class SeatPaymentService {

    private static final int BASE_FARE = 18500;
    private static final int TAXES = 1250;
    private static final int SAVINGS = 950;
    private static final int WINDOW_CHARGE = 700;
    private static final int AISLE_CHARGE = 500;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter STORED_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SeatInventoryRepository seatInventoryRepository;
    private final PaymentRepository paymentRepository;
    private final FlightService flightService;
    private final PassengerService passengerService;

    @Autowired
    public SeatPaymentService(SeatInventoryRepository seatInventoryRepository,
                              PaymentRepository paymentRepository,
                              FlightService flightService,
                              PassengerService passengerService) {
        this.seatInventoryRepository = seatInventoryRepository;
        this.paymentRepository = paymentRepository;
        this.flightService = flightService;
        this.passengerService = passengerService;
    }

    // ---------------------------------------------------------------------
    // Seat inventory
    // ---------------------------------------------------------------------

    public List<SeatInventory> getSeatsByFlight(Long flightId) {
        return seatInventoryRepository.findByFlightIdOrderBySeatNumberAsc(toInt(flightId));
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

    public boolean isSeatBooked(Long flightId, String seatNumber) {
        return seatInventoryRepository.findByFlightIdAndSeatNumber(toInt(flightId), seatNumber)
                .map(seat -> seat.getSeatStatus() == SeatStatus.BOOKED)
                .orElse(false);
    }

    @Transactional
    public boolean confirmSeatSelection(Long flightId, String seatNumber) {
        Optional<SeatInventory> seatOpt =
                seatInventoryRepository.findByFlightIdAndSeatNumber(toInt(flightId), seatNumber);
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
        Optional<SeatInventory> seatOpt =
                seatInventoryRepository.findByFlightIdAndSeatNumber(toInt(flightId), seatNumber);
        if (seatOpt.isPresent()) {
            SeatInventory seat = seatOpt.get();
            seat.setSeatStatus(SeatStatus.AVAILABLE);
            seatInventoryRepository.save(seat);
        }
    }

    // ---------------------------------------------------------------------
    // Payment & booking
    // ---------------------------------------------------------------------

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Optional<Payment> getPaymentById(int id) {
        return paymentRepository.findById(id);
    }

    public Payment savePayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    public Optional<Payment> findByFlightIdAndSeatNumber(Integer flightId, String seatNumber) {
        return paymentRepository.findByFlightIdAndSeatNumber(flightId, seatNumber);
    }

    public void deletePayment(int id) {
        paymentRepository.deleteById(id);
    }

    @Transactional
    public BookingResult confirmBooking(Long flightId, String seatNumber) {
        if (flightId == null || seatNumber == null || seatNumber.isBlank()) {
            return BookingResult.failure("Missing flight or seat information.");
        }

        boolean justBooked = confirmSeatSelection(flightId, seatNumber);

        if (!justBooked) {
            Payment existing = findByFlightIdAndSeatNumber(flightId.intValue(), seatNumber).orElse(null);
            if (existing != null) {
                return BookingResult.success(existing.getBookingId(), existing.getSeatNumber(),
                        existing.getTotalAmount());
            }
            return BookingResult.failure("Sorry, this seat is no longer available. Please choose another seat.");
        }

        Payment saved = savePayment(buildBooking(flightId, seatNumber));
        return BookingResult.success(saved.getBookingId(), saved.getSeatNumber(), saved.getTotalAmount());
    }

    public Payment buildBooking(Long flightId, String seatNumber) {
        int seatCharges = calculateSeatCharge(seatNumber);
        int totalAmount = BASE_FARE + seatCharges + TAXES;

        Flight flight = flightService.getFlight(flightId);
        Passenger passenger = passengerService.getPassenger();

        Payment booking = new Payment();
        booking.setBaseFare(String.valueOf(BASE_FARE));
        booking.setSeatCharges(String.valueOf(seatCharges));
        booking.setTaxes(String.valueOf(TAXES));
        booking.setTotalAmount(String.valueOf(totalAmount));
        booking.setSavings(String.valueOf(SAVINGS));
        booking.setSeatNumber(seatNumber);
        if (flightId != null) {
            booking.setFlightId(flightId.intValue());
        }

        if (flight != null) {
            booking.setFlightNumber(flight.getFlightNumber());
            booking.setSource(flight.getOrigin());
            booking.setDestination(flight.getDestination());
            booking.setDepartureTime(formatStored(flight.getDepartureTime()));
            booking.setArrivalTime(formatStored(flight.getArrivalTime()));
        }
        if (passenger != null) {
            booking.setPassengerName(passenger.getName());
        }
        return booking;
    }

    /**
     * Flight times are stored as strings (yyyy-MM-dd HH:mm); parse and re-format
     * them to a friendly time, falling back to the raw value (or "-") on failure.
     */
    private static String formatStored(String stored) {
        if (stored == null || stored.isBlank()) {
            return "-";
        }
        try {
            return LocalDateTime.parse(stored, STORED_FMT).format(TIME_FMT);
        } catch (DateTimeParseException ex) {
            return stored;
        }
    }

    private int calculateSeatCharge(String seatNumber) {
        if (seatNumber == null || seatNumber.length() < 2) {
            return 0;
        }
        try {
            int col = Integer.parseInt(seatNumber.substring(1));
            switch (col) {
                case 1:
                case 4:
                    return WINDOW_CHARGE;
                case 2:
                case 3:
                    return AISLE_CHARGE;
                default:
                    return 0;
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Integer toInt(Long flightId) {
        return flightId == null ? null : flightId.intValue();
    }
}

