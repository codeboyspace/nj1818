package com.genc.arfoms.booking.service;

import com.genc.arfoms.booking.client.FlightClient;
import com.genc.arfoms.booking.model.Booking;
import com.genc.arfoms.booking.model.BookingStatus;
import com.genc.arfoms.booking.repository.BookingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FlightClient flightClient;

    public BookingService(BookingRepository bookingRepository, FlightClient flightClient) {
        this.bookingRepository = bookingRepository;
        this.flightClient = flightClient;
    }

    public Booking createBooking(Booking booking) {
        try {
            flightClient.verifyFlightExists(booking.getFlightId());
        } catch (feign.FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Flight does not exist");
        }
        booking.setPnr(generatePnr());
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        return bookingRepository.save(booking);
    }

    public Booking selectSeat(Long bookingId, String seatNumber) {
        Booking booking = getBooking(bookingId);
        booking.setSeatNumber(seatNumber);
        return bookingRepository.save(booking);
    }

    public Booking modifyBooking(Long bookingId, String passengerName, String seatNumber) {
        Booking booking = getBooking(bookingId);
        booking.setPassengerName(passengerName);
        booking.setSeatNumber(seatNumber);
        return bookingRepository.save(booking);
    }

    public Booking cancelBooking(Long bookingId) {
        Booking booking = getBooking(bookingId);
        booking.setBookingStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    public Booking getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    public List<Booking> getAll() {
        return bookingRepository.findAll();
    }

    private String generatePnr() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}

