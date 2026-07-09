package com.genc.arfoms.checkin.client;

import com.genc.arfoms.checkin.dto.FlightView;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Talks to the flight-service so the check-in screen can show the real
 * flight (number + route) tied to a booking instead of hardcoding it.
 */
@Component
public class FlightClient {

    private final RestClient restClient;

    public FlightClient(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder loadBalancedRestClientBuilder,
                        @Value("${arfoms.flight-service.base-url}") String baseUrl) {
        this.restClient = loadBalancedRestClientBuilder.baseUrl(baseUrl).build();
    }

    /** Fetches the live flight, or {@code null} if it does not exist. */
    public FlightView getFlight(Long flightId) {
        if (flightId == null) {
            return null;
        }
        try {
            return restClient.get()
                    .uri("/api/flights/{id}", flightId)
                    .retrieve()
                    .body(FlightView.class);
        } catch (Exception ex) {
            return null;
        }
    }
}




