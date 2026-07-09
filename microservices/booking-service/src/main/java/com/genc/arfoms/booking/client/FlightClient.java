package com.genc.arfoms.booking.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FlightClient {

    private final RestClient restClient;

    public FlightClient(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder loadBalancedRestClientBuilder,
                        @Value("${arfoms.flight-service.base-url}") String baseUrl) {
        this.restClient = loadBalancedRestClientBuilder.baseUrl(baseUrl).build();
    }

    public boolean flightExists(Long flightId) {
        try {
            restClient.get().uri("/api/flights/{id}", flightId).retrieve().toBodilessEntity();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}

