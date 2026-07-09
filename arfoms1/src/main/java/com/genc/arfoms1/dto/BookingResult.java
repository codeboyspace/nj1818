package com.genc.arfoms1.dto;

import lombok.Getter;


@Getter
public class BookingResult {

    private final boolean success;
    private final String message;
    private final Integer bookingId;
    private final String seatNumber;
    private final String totalAmount;

    private BookingResult(boolean success, String message, Integer bookingId,
                          String seatNumber, String totalAmount) {
        this.success = success;
        this.message = message;
        this.bookingId = bookingId;
        this.seatNumber = seatNumber;
        this.totalAmount = totalAmount;
    }

    public static BookingResult failure(String message) {
        return new BookingResult(false, message, null, null, null);
    }

    public static BookingResult success(Integer bookingId, String seatNumber, String totalAmount) {
        return new BookingResult(true, "Your booking has been confirmed successfully!",
                bookingId, seatNumber, totalAmount);
    }
}

