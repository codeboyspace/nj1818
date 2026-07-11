package com.genc.arfoms.booking.controller;

import com.genc.arfoms.booking.model.Booking;
import com.genc.arfoms.booking.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final Logger logger = LoggerFactory.getLogger(BookingController.class);
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public Booking createBooking(@RequestBody Booking booking) {
        logger.info("Received request to create booking: {}", booking);
        return bookingService.createBooking(booking);
    }

    @PatchMapping("/{bookingId}/seat")
    public Booking selectSeat(@PathVariable Long bookingId, @RequestBody SelectSeatRequest request) {
        logger.info("Received request to select seat '{}' for booking ID: {}", request.seatNumber(), bookingId);
        return bookingService.selectSeat(bookingId, request.seatNumber());
    }

    @PatchMapping("/{bookingId}")
    public Booking modifyBooking(@PathVariable Long bookingId, @RequestBody ModifyBookingRequest request) {
        logger.info("Received request to modify booking ID: {} with passengerName: '{}', seatNumber: '{}'", bookingId, request.passengerName(), request.seatNumber());
        return bookingService.modifyBooking(bookingId, request.passengerName(), request.seatNumber());
    }

    @PatchMapping("/{bookingId}/cancel")
    public Booking cancelBooking(@PathVariable Long bookingId) {
        logger.info("Received request to cancel booking ID: {}", bookingId);
        return bookingService.cancelBooking(bookingId);
    }

    @GetMapping("/{bookingId}")
    public Booking getBooking(@PathVariable Long bookingId) {
        logger.info("Received request to fetch booking ID: {}", bookingId);
        return bookingService.getBooking(bookingId);
    }

    @GetMapping
    public List<Booking> getAllBookings() {
        logger.info("Received request to fetch all bookings");
        return bookingService.getAll();
    }

    public record SelectSeatRequest(String seatNumber) {
    }

    public record ModifyBookingRequest(String passengerName, String seatNumber) {
    }
}

