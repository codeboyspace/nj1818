package com.genc.arfoms.crew.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

@Component
public class FlightClient {

    private final RestClient restClient;

    public FlightClient(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder loadBalancedRestClientBuilder,
                        @Value("${arfoms.flight-service.base-url}") String baseUrl) {
        this.restClient = loadBalancedRestClientBuilder.baseUrl(baseUrl).build();
    }

    public boolean flightExists(Long flightId) {
        if (flightId == null) {
            return false;
        }
        try {
            restClient.get().uri("/api/flights/{id}", flightId).retrieve().toBodilessEntity();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public FlightView getFlight(Long flightId) {
        if (flightId == null) {
            return null;
        }
        try {
            return restClient.get().uri("/api/flights/{id}", flightId).retrieve().body(FlightView.class);
        } catch (Exception ex) {
            return null;
        }
    }

    public record FlightView(Long flightId, LocalDateTime departureTime, LocalDateTime arrivalTime) {
    }
}

