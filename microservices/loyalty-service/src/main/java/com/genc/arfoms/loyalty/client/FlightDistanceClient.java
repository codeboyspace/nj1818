package com.genc.arfoms.loyalty.client;

import com.genc.arfoms.loyalty.dto.FlightDistance;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/**
 * Calls the flight-service to obtain the great-circle distance (in miles)
 * between a flight's origin and destination airports. Used by the loyalty
 * manager to tailor offers to how far a member has flown.
 */
@Component
public class FlightDistanceClient {

    private final RestClient restClient;

    public FlightDistanceClient(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder loadBalancedRestClientBuilder,
                                @Value("${arfoms.flight-service.base-url}") String baseUrl) {
        this.restClient = loadBalancedRestClientBuilder.baseUrl(baseUrl).build();
    }

    /** Fetches the distance in miles for the given flight from flight-service. */
    public double distanceForFlight(Long flightId) {
        try {
            FlightDistance distance = restClient.get()
                    .uri("/api/flights/{id}/distance", flightId)
                    .retrieve()
                    .body(FlightDistance.class);
            if (distance == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Flight service returned no distance for flight " + flightId);
            }
            return distance.distanceMiles();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Unable to obtain distance for flight " + flightId + ": " + ex.getMessage());
        }
    }
}

