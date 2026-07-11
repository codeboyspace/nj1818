package com.genc.arfoms.booking.service;

import com.genc.arfoms.booking.client.FlightClient;
import com.genc.arfoms.booking.model.Booking;
import com.genc.arfoms.booking.model.BookingStatus;
import com.genc.arfoms.booking.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.genc.arfoms.booking.exception.NoDataFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final Logger logger = LoggerFactory.getLogger(BookingService.class);
    private final BookingRepository bookingRepository;
    private final FlightClient flightClient;

    public BookingService(BookingRepository bookingRepository, FlightClient flightClient) {
        this.bookingRepository = bookingRepository;
        this.flightClient = flightClient;
    }

    public Booking createBooking(Booking booking) {
        logger.info("Creating booking for flight ID: {}, passenger: {}", booking.getFlightId(), booking.getPassengerName());
        try {
            flightClient.verifyFlightExists(booking.getFlightId());
            logger.info("Flight ID: {} verified successfully via Feign client", booking.getFlightId());
        } catch (feign.FeignException.NotFound e) {
            logger.error("Booking creation failed: Flight ID {} not found", booking.getFlightId());
            throw new IllegalArgumentException("Flight does not exist");
        }
        booking.setPnr(generatePnr());
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        Booking savedBooking = bookingRepository.save(booking);
        logger.info("Booking created successfully with ID: {}, PNR: {}", savedBooking.getBookingId(), savedBooking.getPnr());
        return savedBooking;
    }

    public Booking selectSeat(Long bookingId, String seatNumber) {
        logger.info("Selecting seat '{}' for booking ID: {}", seatNumber, bookingId);
        Booking booking = getBooking(bookingId);
        booking.setSeatNumber(seatNumber);
        Booking savedBooking = bookingRepository.save(booking);
        logger.info("Seat '{}' successfully assigned for booking ID: {}", seatNumber, bookingId);
        return savedBooking;
    }

    public Booking modifyBooking(Long bookingId, String passengerName, String seatNumber) {
        logger.info("Modifying booking ID: {}, passengerName: '{}', seatNumber: '{}'", bookingId, passengerName, seatNumber);
        Booking booking = getBooking(bookingId);
        booking.setPassengerName(passengerName);
        booking.setSeatNumber(seatNumber);
        Booking savedBooking = bookingRepository.save(booking);
        logger.info("Booking ID: {} modified successfully.", bookingId);
        return savedBooking;
    }

    public Booking cancelBooking(Long bookingId) {
        logger.info("Cancelling booking ID: {}", bookingId);
        Booking booking = getBooking(bookingId);
        booking.setBookingStatus(BookingStatus.CANCELLED);
        Booking savedBooking = bookingRepository.save(booking);
        logger.info("Booking ID: {} cancelled successfully.", bookingId);
        return savedBooking;
    }

    public Booking getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    logger.warn("Booking lookup failed: Booking ID {} not found", bookingId);
                    return new NoDataFoundException("Booking not found");
                });
    }

    public List<Booking> getAll() {
        logger.info("Fetching all bookings from database.");
        return bookingRepository.findAll();
    }

    private String generatePnr() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}

