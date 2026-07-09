package com.genc.arfoms1;

import com.genc.arfoms1.controller.FlightController;
import com.genc.arfoms1.exception.FlightException;
import com.genc.arfoms1.model.Flight;
import com.genc.arfoms1.model.IndianAirports;
import com.genc.arfoms1.repository.FlightRepository;
import com.genc.arfoms1.repository.SeatInventoryRepository;
import com.genc.arfoms1.service.FlightService;
import com.genc.arfoms1.service.FlightServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlightDispatcherValidationTest {

    private static final DateTimeFormatter REQUEST_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Test
    void metadataExposesIndianAndInternationalAirports() {
        FlightService flightService = mock(FlightService.class);
        FlightController controller = new FlightController(flightService);

        Map<String, Object> metadata = controller.getMetadata();

        @SuppressWarnings("unchecked")
        List<IndianAirports.Airport> allAirports = (List<IndianAirports.Airport>) metadata.get("airports");
        @SuppressWarnings("unchecked")
        List<IndianAirports.Airport> indianAirports = (List<IndianAirports.Airport>) metadata.get("indianAirports");
        @SuppressWarnings("unchecked")
        List<IndianAirports.Airport> internationalAirports = (List<IndianAirports.Airport>) metadata.get("internationalAirports");

        assertThat(indianAirports).extracting(IndianAirports.Airport::code).contains("DEL");
        assertThat(internationalAirports).extracting(IndianAirports.Airport::code).contains("DXB", "BKK");
        assertThat(allAirports).extracting(IndianAirports.Airport::code).contains("DEL", "DXB", "BKK");
    }

    @Test
    void addFlightAcceptsIndianToInternationalRoute() {
        FlightRepository flightRepository = mock(FlightRepository.class);
        SeatInventoryRepository seatInventoryRepository = mock(SeatInventoryRepository.class);
        FlightServiceImpl service = new FlightServiceImpl(flightRepository, seatInventoryRepository);
        Flight flight = buildFlight("DEL", "DXB", LocalDateTime.now().plusDays(1));
        flight.setSeatRows(1);
        flight.setSeatColumns(2);
        flight.setSeatCount(2);

        when(flightRepository.findByFlightNumberIgnoreCase("AI-101")).thenReturn(Optional.empty());
        when(flightRepository.save(any(Flight.class))).thenAnswer(invocation -> {
            Flight savedFlight = invocation.getArgument(0);
            savedFlight.setFlightId(1L);
            return savedFlight;
        });

        service.addFlight(flight);

        verify(flightRepository).save(any(Flight.class));
        verify(seatInventoryRepository).saveAll(any());
        assertThat(flight.getOrigin()).isEqualTo("DEL");
        assertThat(flight.getDestination()).isEqualTo("DXB");
        assertThat(flight.getDistanceMiles()).isBetween(1200.0, 1500.0);
    }

    @Test
    void addFlightRejectsPastDepartureDateTime() {
        FlightRepository flightRepository = mock(FlightRepository.class);
        SeatInventoryRepository seatInventoryRepository = mock(SeatInventoryRepository.class);
        FlightServiceImpl service = new FlightServiceImpl(flightRepository, seatInventoryRepository);
        Flight flight = buildFlight("DEL", "BOM", LocalDateTime.now().minusHours(3));

        assertThatThrownBy(() -> service.addFlight(flight))
                .isInstanceOf(FlightException.class)
                .hasMessageContaining("departureTime cannot be in the past");
    }

    private Flight buildFlight(String origin, String destination, LocalDateTime departureTime) {
        Flight flight = new Flight();
        flight.setFlightNumber("AI-101");
        flight.setFlightName("Integration Test Flight");
        flight.setOrigin(origin);
        flight.setDestination(destination);
        flight.setDepartureTime(departureTime.format(REQUEST_DATE_TIME_FORMAT));
        flight.setArrivalTime(departureTime.plusHours(4).format(REQUEST_DATE_TIME_FORMAT));
        flight.setEconomyFare(5500.0);
        flight.setPremiumFare(8200.0);
        flight.setFirstFare(12500.0);
        flight.setFare(5500.0);
        return flight;
    }
}

