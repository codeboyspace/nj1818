package com.genc.arfoms1.controller;

import com.genc.arfoms1.model.Booking;
import com.genc.arfoms1.model.Flight;
import com.genc.arfoms1.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for the flight booking module.
 * All endpoints exchange JSON. The HTML pages (served as static resources)
 * call these endpoints with fetch().
 */
@RestController
@RequestMapping("/flights")
@CrossOrigin
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /** List all available flights (browser-friendly GET, e.g. /flights). */
    @GetMapping
    public List<Flight> listFlights() {
        log.info("GET /flights - listing all available flights");
        return bookingService.searchAvailableFlights(new Booking());
    }

    /** Search available flights for the given criteria. */
    @PostMapping("/search")
    public List<Flight> searchFlights(@RequestBody Booking searchCriteria) {
        log.info("POST /flights/search - from={}, to={}, date={}, type={}",
                searchCriteria.getFromLocation(), searchCriteria.getToLocation(),
                searchCriteria.getDepartureDate(), searchCriteria.getFlightType());
        return bookingService.searchAvailableFlights(searchCriteria);
    }

    /** Prepare a booking draft (ensures passenger slots) for the passenger-details page. */
    @PostMapping("/passenger")
    public Booking prepareDraft(@RequestBody Booking booking) {
        log.info("POST /flights/passenger - preparing booking draft");
        return bookingService.prepareBookingDraft(booking);
    }

    /** Persist a new booking and return the saved record (including generated id and PNR). */
    @PostMapping("/passenger/confirm")
    public Booking createBooking(@RequestBody Booking booking) {
        log.info("POST /flights/passenger/confirm - creating new booking");
        return bookingService.createBooking(booking);
    }

    /** Fetch a single booking by id for the confirmation page. */
    @GetMapping("/confirmation/{bookingId}")
    public Booking getConfirmation(@PathVariable Long bookingId) {
        log.info("GET /flights/confirmation/{} - fetching booking confirmation", bookingId);
        return bookingService.getBookingById(bookingId);
    }

    /** Fetch the most recent booking for the manage-booking dashboard. */
    @GetMapping("/manage")
    public Booking manage() {
        log.info("GET /flights/manage - fetching most recent booking");
        return bookingService.getBookingDetails();
    }

    /** Fetch all past bookings (most recent first) for the manage-booking dashboard. */
    @GetMapping("/bookings")
    public List<Booking> allBookings() {
        log.info("GET /flights/bookings - fetching all bookings");
        return bookingService.getAllBookings();
    }

    /** Modify an existing booking (date / seat / etc.). */
    @PostMapping("/{bookingId}/modify")
    public Booking modify(@PathVariable Long bookingId, @RequestBody Booking updatedBooking) {
        log.info("POST /flights/{}/modify - modifying booking", bookingId);
        return bookingService.modifyBooking(bookingId, updatedBooking);
    }

    /** Cancel an existing booking. */
    @PostMapping("/{bookingId}/cancel")
    public Booking cancel(@PathVariable Long bookingId) {
        log.info("POST /flights/{}/cancel - cancelling booking", bookingId);
        return bookingService.cancelBooking(bookingId);
    }
}
