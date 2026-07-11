package com.genc.arfoms.flight.service;

import com.genc.arfoms.flight.dto.FlightDistanceResponse;
import com.genc.arfoms.flight.model.Flight;
import com.genc.arfoms.flight.model.FlightStatus;
import com.genc.arfoms.flight.repository.FlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.genc.arfoms.flight.exception.NoDataFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure JUnit 5 + Mockito unit tests for the full flight-service flow:
 * posting (create/update/delete) and fetching (read) operations.
 * No Spring context or database is started - all collaborators are mocked.
 */
@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private AirportDistanceService airportDistanceService;

    @InjectMocks
    private FlightService flightService;

    private Flight sampleFlight;

    @BeforeEach
    void setUp() {
        sampleFlight = buildFlight(1L, "AI2051", "DEL", "DXB",
                LocalDateTime.of(2026, 7, 15, 2, 30),
                LocalDateTime.of(2026, 7, 15, 5, 10));
    }

    // ---------------------------------------------------------------------
    //  POSTING  (create)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("addFlight persists a valid flight and returns it")
    void addFlight_valid_savesAndReturns() {
        when(flightRepository.save(any(Flight.class))).thenAnswer(inv -> inv.getArgument(0));

        Flight saved = flightService.addFlight(sampleFlight);

        assertThat(saved).isSameAs(sampleFlight);
        verify(flightRepository).save(sampleFlight);
    }

    @Test
    @DisplayName("addFlight rejects when arrival is not after departure")
    void addFlight_invalidTimes_throwsAndDoesNotSave() {
        Flight bad = buildFlight(null, "XX9999", "DEL", "BOM",
                LocalDateTime.of(2026, 7, 15, 10, 0),
                LocalDateTime.of(2026, 7, 15, 9, 0)); // arrival before departure

        assertThatThrownBy(() -> flightService.addFlight(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Arrival time must be after departure time");

        verify(flightRepository, never()).save(any());
    }

    @Test
    @DisplayName("addFlight rejects null times")
    void addFlight_nullTimes_throws() {
        Flight bad = buildFlight(null, "XX0000", "DEL", "BOM", null, null);

        assertThatThrownBy(() -> flightService.addFlight(bad))
                .isInstanceOf(IllegalArgumentException.class);

        verify(flightRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    //  FETCHING  (read)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("getAllFlights returns every flight from the repository")
    void getAllFlights_returnsList() {
        Flight second = buildFlight(2L, "6E1002", "BOM", "BLR",
                LocalDateTime.of(2026, 7, 15, 9, 0),
                LocalDateTime.of(2026, 7, 15, 10, 45));
        when(flightRepository.findAll()).thenReturn(List.of(sampleFlight, second));

        List<Flight> flights = flightService.getAllFlights();

        assertThat(flights).hasSize(2).containsExactly(sampleFlight, second);
        verify(flightRepository).findAll();
    }

    @Test
    @DisplayName("getFlightDetails returns the flight when it exists")
    void getFlightDetails_found() {
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));

        Flight found = flightService.getFlightDetails(1L);

        assertThat(found).isSameAs(sampleFlight);
    }

    @Test
    @DisplayName("getFlightDetails throws 404 when the flight is missing")
    void getFlightDetails_notFound() {
        when(flightRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightService.getFlightDetails(99L))
                .isInstanceOf(NoDataFoundException.class)
                .hasMessageContaining("Flight not found");
    }

    @Test
    @DisplayName("getByFlightNumber returns the flight when it exists")
    void getByFlightNumber_found() {
        when(flightRepository.findByFlightNumber("AI2051")).thenReturn(Optional.of(sampleFlight));

        Flight found = flightService.getByFlightNumber("AI2051");

        assertThat(found.getOrigin()).isEqualTo("DEL");
        assertThat(found.getDestination()).isEqualTo("DXB");
    }

    @Test
    @DisplayName("getByFlightNumber throws 404 when the flight is missing")
    void getByFlightNumber_notFound() {
        when(flightRepository.findByFlightNumber("NONE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightService.getByFlightNumber("NONE"))
                .isInstanceOf(NoDataFoundException.class);
    }

    // ---------------------------------------------------------------------
    //  POSTING  (update / delete)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("updateSchedule changes the times and persists")
    void updateSchedule_updatesTimes() {
        LocalDateTime newDep = LocalDateTime.of(2026, 7, 16, 6, 0);
        LocalDateTime newArr = LocalDateTime.of(2026, 7, 16, 8, 30);
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));
        when(flightRepository.save(any(Flight.class))).thenAnswer(inv -> inv.getArgument(0));

        Flight updated = flightService.updateSchedule(1L, newDep, newArr);

        assertThat(updated.getDepartureTime()).isEqualTo(newDep);
        assertThat(updated.getArrivalTime()).isEqualTo(newArr);
        verify(flightRepository).save(sampleFlight);
    }

    @Test
    @DisplayName("updateSchedule rejects invalid time ordering before touching the DB")
    void updateSchedule_invalidTimes_throws() {
        LocalDateTime dep = LocalDateTime.of(2026, 7, 16, 10, 0);
        LocalDateTime arr = LocalDateTime.of(2026, 7, 16, 9, 0);

        assertThatThrownBy(() -> flightService.updateSchedule(1L, dep, arr))
                .isInstanceOf(IllegalArgumentException.class);

        verify(flightRepository, never()).findById(any());
        verify(flightRepository, never()).save(any());
    }

    @Test
    @DisplayName("setFareClass updates all three fare tiers")
    void setFareClass_updatesFares() {
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));
        when(flightRepository.save(any(Flight.class))).thenAnswer(inv -> inv.getArgument(0));

        Flight updated = flightService.setFareClass(1L,
                new BigDecimal("28000"), new BigDecimal("50400"), new BigDecimal("84000"));

        assertThat(updated.getEconomyFare()).isEqualByComparingTo("28000");
        assertThat(updated.getBusinessFare()).isEqualByComparingTo("50400");
        assertThat(updated.getFirstFare()).isEqualByComparingTo("84000");
    }

    @Test
    @DisplayName("updateStatus changes the flight status")
    void updateStatus_changesStatus() {
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));
        when(flightRepository.save(any(Flight.class))).thenAnswer(inv -> inv.getArgument(0));

        Flight updated = flightService.updateStatus(1L, FlightStatus.BOARDING);

        assertThat(updated.getFlightStatus()).isEqualTo(FlightStatus.BOARDING);
    }

    @Test
    @DisplayName("deleteFlight removes an existing flight")
    void deleteFlight_existing() {
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));

        flightService.deleteFlight(1L);

        ArgumentCaptor<Flight> captor = ArgumentCaptor.forClass(Flight.class);
        verify(flightRepository).delete(captor.capture());
        assertThat(captor.getValue()).isSameAs(sampleFlight);
    }

    @Test
    @DisplayName("deleteFlight throws 404 when the flight does not exist")
    void deleteFlight_missing() {
        when(flightRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightService.deleteFlight(5L))
                .isInstanceOf(NoDataFoundException.class);

        verify(flightRepository, never()).delete(any());
    }

    // ---------------------------------------------------------------------
    //  DISTANCE  (delegation)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("getFlightDistance combines flight data with the distance service")
    void getFlightDistance_computesResponse() {
        when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));
        when(airportDistanceService.distanceMiles("DEL", "DXB")).thenReturn(1370.5);

        FlightDistanceResponse response = flightService.getFlightDistance(1L);

        assertThat(response.flightId()).isEqualTo(1L);
        assertThat(response.flightNumber()).isEqualTo("AI2051");
        assertThat(response.origin()).isEqualTo("DEL");
        assertThat(response.destination()).isEqualTo("DXB");
        assertThat(response.distanceMiles()).isEqualTo(1370.5);
    }

    @Test
    @DisplayName("distanceBetween delegates to the distance service")
    void distanceBetween_delegates() {
        when(airportDistanceService.distanceMiles("DEL", "JFK")).thenReturn(7300.0);

        double miles = flightService.distanceBetween("DEL", "JFK");

        assertThat(miles).isEqualTo(7300.0);
        verify(airportDistanceService).distanceMiles("DEL", "JFK");
    }

    // ---------------------------------------------------------------------
    //  helpers
    // ---------------------------------------------------------------------

    private Flight buildFlight(Long id, String number, String origin, String destination,
                               LocalDateTime dep, LocalDateTime arr) {
        Flight f = new Flight();
        f.setFlightId(id);
        f.setFlightNumber(number);
        f.setOrigin(origin);
        f.setDestination(destination);
        f.setDepartureTime(dep);
        f.setArrivalTime(arr);
        f.setFlightStatus(FlightStatus.SCHEDULED);
        f.setEconomyFare(new BigDecimal("5000"));
        f.setBusinessFare(new BigDecimal("9000"));
        f.setFirstFare(new BigDecimal("15000"));
        return f;
    }
}

