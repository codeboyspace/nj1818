package com.genc.arfoms1.service;

import com.genc.arfoms1.model.Booking;
import com.genc.arfoms1.model.Flight;

import java.util.List;

public interface BookingService {

    List<Flight> searchAvailableFlights(Booking searchCriteria);

    Booking prepareBookingDraft(Booking booking);


    Booking createBooking(Booking booking);

    Booking selectSeat(Long bookingId, String seat);

    Booking modifyBooking(Long bookingId, Booking updatedBooking);

    Booking cancelBooking(Long bookingId);

    Booking getBookingDetails();

    Booking getBookingById(Long bookingId);

    List<Booking> getAllBookings();
}
