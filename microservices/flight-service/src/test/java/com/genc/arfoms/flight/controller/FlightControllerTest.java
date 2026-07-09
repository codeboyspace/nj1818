package com.genc.arfoms.flight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.genc.arfoms.flight.model.Flight;
import com.genc.arfoms.flight.model.FlightStatus;
import com.genc.arfoms.flight.service.FlightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JUnit 5 + Mockito web-layer tests for the flight endpoints, driving the
 * controller through a standalone {@link MockMvc} (no full Spring context / DB).
 * Verifies both the fetching (GET) and posting (POST) HTTP flows.
 */
@ExtendWith(MockitoExtension.class)
class FlightControllerTest {

    @Mock
    private FlightService flightService;

    @InjectMocks
    private FlightController flightController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(flightController)
                .setMessageConverters(converter)
                .build();
    }

    @Test
    @DisplayName("GET /api/flights returns the list of flights (fetching)")
    void getAllFlights_returnsJsonArray() throws Exception {
        when(flightService.getAllFlights()).thenReturn(List.of(
                flight(1L, "AI2051", "DEL", "DXB"),
                flight(2L, "6E1002", "BOM", "BLR")));

        mockMvc.perform(get("/api/flights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].flightNumber").value("AI2051"))
                .andExpect(jsonPath("$[0].origin").value("DEL"))
                .andExpect(jsonPath("$[1].destination").value("BLR"));

        verify(flightService).getAllFlights();
    }

    @Test
    @DisplayName("GET /api/flights/{id} returns a single flight (fetching)")
    void getFlightDetails_returnsFlight() throws Exception {
        when(flightService.getFlightDetails(1L)).thenReturn(flight(1L, "AI2051", "DEL", "DXB"));

        mockMvc.perform(get("/api/flights/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightId").value(1))
                .andExpect(jsonPath("$.flightNumber").value("AI2051"));

        verify(flightService).getFlightDetails(1L);
    }

    @Test
    @DisplayName("POST /api/flights creates a flight (posting)")
    void addFlight_createsFlight() throws Exception {
        Flight request = flight(null, "AI9001", "DEL", "SIN");
        Flight saved = flight(10L, "AI9001", "DEL", "SIN");
        when(flightService.addFlight(any(Flight.class))).thenReturn(saved);

        mockMvc.perform(post("/api/flights")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightId").value(10))
                .andExpect(jsonPath("$.flightNumber").value("AI9001"))
                .andExpect(jsonPath("$.origin").value("DEL"))
                .andExpect(jsonPath("$.destination").value("SIN"));

        verify(flightService).addFlight(any(Flight.class));
    }

    @Test
    @DisplayName("PATCH /api/flights/{id}/status updates the flight status (posting)")
    void updateStatus_changesStatus() throws Exception {
        Flight updated = flight(1L, "AI2051", "DEL", "DXB");
        updated.setFlightStatus(FlightStatus.BOARDING);
        when(flightService.updateStatus(eq(1L), eq(FlightStatus.BOARDING))).thenReturn(updated);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/flights/1/status")
                        .contentType("application/json")
                        .content("{\"flightStatus\":\"BOARDING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightStatus").value("BOARDING"));

        verify(flightService).updateStatus(1L, FlightStatus.BOARDING);
    }

    @Test
    @DisplayName("DELETE /api/flights/{id} removes a flight (posting)")
    void deleteFlight_invokesService() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/flights/1"))
                .andExpect(status().isOk());

        verify(flightService).deleteFlight(1L);
    }

    private Flight flight(Long id, String number, String origin, String destination) {
        Flight f = new Flight();
        f.setFlightId(id);
        f.setFlightNumber(number);
        f.setOrigin(origin);
        f.setDestination(destination);
        f.setDepartureTime(LocalDateTime.of(2026, 7, 15, 2, 30));
        f.setArrivalTime(LocalDateTime.of(2026, 7, 15, 5, 10));
        f.setFlightStatus(FlightStatus.SCHEDULED);
        f.setEconomyFare(new BigDecimal("28000"));
        f.setBusinessFare(new BigDecimal("50400"));
        f.setFirstFare(new BigDecimal("84000"));
        return f;
    }
}

