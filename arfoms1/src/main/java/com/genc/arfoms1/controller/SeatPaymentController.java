package com.genc.arfoms1.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.genc.arfoms1.dto.BookingResult;
import com.genc.arfoms1.model.Flight;
import com.genc.arfoms1.model.Passenger;
import com.genc.arfoms1.model.Payment;
import com.genc.arfoms1.model.SeatInventory;
import com.genc.arfoms1.model.enums.SeatStatus;
import com.genc.arfoms1.service.FlightService;
import com.genc.arfoms1.service.PassengerService;
import com.genc.arfoms1.service.SeatPaymentService;

/**
 * REST API for the seat-selection &amp; payment module.
 * <p>
 * Combines what used to be two separate controllers
 * ({@code SeatInventoryController} + {@code PaymentController}) into one,
 * since they serve a single end-to-end flow:
 * <pre>
 *   select seat  →  view fare  →  confirm booking
 * </pre>
 * All endpoints exchange JSON; the static HTML pages call them with fetch().
 */
@RestController
@CrossOrigin(origins = "*")
public class SeatPaymentController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter STORED_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String KEY_SUCCESS = "success";
    private static final String KEY_MESSAGE = "message";

    @Autowired
    private SeatPaymentService seatPaymentService;

    @Autowired
    private FlightService flightService;

    @Autowired
    private PassengerService passengerService;

    // ---------------------------------------------------------------------
    // Seat selection
    // ---------------------------------------------------------------------

    /**
     * Returns all the data the seat-selection page needs as JSON.
     * The static page (seatInventory.html) calls this via fetch() and
     * renders the seats / flight / passenger details on the client side.
     */
    @GetMapping("/airline/api/seats")
    public Map<String, Object> getSeatData(@RequestParam(defaultValue = "1") Long flightId) {

        List<SeatInventory> allSeats = seatPaymentService.getSeatsByFlight(flightId);

        List<String> bookedSeats = allSeats.stream()
                .filter(s -> s.getSeatStatus() == SeatStatus.BOOKED)
                .map(SeatInventory::getSeatNumber)
                .toList();

        Flight flight = flightService.getFlight(flightId);
        Passenger passenger = passengerService.getPassenger();

        Map<String, Object> response = new HashMap<>();
        response.put("flightId", flightId);
        response.put("allSeats", allSeats);
        response.put("bookedSeatNumbers", bookedSeats);
        response.put("flight", flight);
        response.put("passenger", passenger);

        if (flight != null) {
            response.put("departureTime", formatStored(flight.getDepartureTime(), TIME_FMT));
            response.put("arrivalTime", formatStored(flight.getArrivalTime(), TIME_FMT));
            response.put("flightDate", formatStored(flight.getDepartureTime(), DATE_FMT));
        }

        return response;
    }

    /**
     * Flight departure/arrival times are stored as strings (yyyy-MM-dd HH:mm).
     * Parse and re-format them for display, falling back to the raw value
     * (or "-" when absent) if parsing fails.
     */
    private static String formatStored(String stored, DateTimeFormatter target) {
        if (stored == null || stored.isBlank()) {
            return "-";
        }
        try {
            return LocalDateTime.parse(stored, STORED_FMT).format(target);
        } catch (DateTimeParseException ex) {
            return stored;
        }
    }


    /**
     * Returns the booking/fare details as JSON for the given flight &amp; seat.
     * The static payment.html page fetches this and renders it client-side.
     */
    @GetMapping("/api/payment")
    public Payment getBooking(
            @RequestParam(required = false) Long flightId,
            @RequestParam(required = false) String seatNumber) {
        return seatPaymentService.buildBooking(flightId, seatNumber);
    }

    @PostMapping("/booking/confirm")
    public ResponseEntity<Map<String, Object>> confirmBooking(
            @RequestParam(required = false) Long flightId,
            @RequestParam(required = false) String seatNumber) {

        BookingResult result = seatPaymentService.confirmBooking(flightId, seatNumber);

        Map<String, Object> response = new HashMap<>();
        response.put(KEY_SUCCESS, result.isSuccess());
        response.put(KEY_MESSAGE, result.getMessage());

        if (result.isSuccess()) {
            response.put("bookingId", result.getBookingId());
            response.put("seatNumber", result.getSeatNumber());
            response.put("totalAmount", result.getTotalAmount());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
}

