package com.genc.arfoms.checkin.client;

import com.genc.arfoms.checkin.dto.BookingView;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Talks to the booking-service to validate and fetch booking details.
 * This keeps the check-in module free of any hardcoded passenger data: the
 * source of truth stays in the booking-service / database.
 */
@Component
public class BookingClient {

    private final RestClient restClient;

    public BookingClient(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder loadBalancedRestClientBuilder,
                         @Value("${arfoms.booking-service.base-url}") String baseUrl) {
        this.restClient = loadBalancedRestClientBuilder.baseUrl(baseUrl).build();
    }

    /** Fetches the live booking, or {@code null} if it does not exist. */
    public BookingView getBooking(Long bookingId) {
        if (bookingId == null) {
            return null;
        }
        try {
            return restClient.get()
                    .uri("/api/bookings/{id}", bookingId)
                    .retrieve()
                    .body(BookingView.class);
        } catch (Exception ex) {
            return null;
        }
    }

    public boolean bookingExists(Long bookingId) {
        return getBooking(bookingId) != null;
    }
}

