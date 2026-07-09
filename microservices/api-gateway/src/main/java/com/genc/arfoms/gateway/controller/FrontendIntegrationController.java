package com.genc.arfoms.gateway.controller;

import com.genc.arfoms.gateway.client.BookingFeignClient;
import com.genc.arfoms.gateway.client.CrewFeignClient;
import com.genc.arfoms.gateway.client.FlightFeignClient;
import com.genc.arfoms.gateway.client.LoyaltyFeignClient;
import com.genc.arfoms.gateway.client.AuthFeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class FrontendIntegrationController {

    private static final Set<String> INTERNATIONAL_AIRPORTS = Set.of("DXB", "SIN", "LHR", "JFK", "BKK");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Map<String, String> AIRLINE_NAMES = Map.of(
            "AI", "Air India",
            "6E", "IndiGo",
            "UK", "Vistara",
            "SG", "SpiceJet",
            "G8", "Go First",
            "I5", "AirAsia India",
            "QP", "Akasa Air"
    );

    private final FlightFeignClient flightClient;
    private final BookingFeignClient bookingClient;
    private final CrewFeignClient crewClient;
    private final LoyaltyFeignClient loyaltyClient;
    private final AuthFeignClient authClient;
    private final Map<Long, Map<String, Object>> bookingViewState = new ConcurrentHashMap<>();

    public FrontendIntegrationController(
            FlightFeignClient flightClient,
            BookingFeignClient bookingClient,
            CrewFeignClient crewClient,
            LoyaltyFeignClient loyaltyClient,
            AuthFeignClient authClient) {
        this.flightClient = flightClient;
        this.bookingClient = bookingClient;
        this.crewClient = crewClient;
        this.loyaltyClient = loyaltyClient;
        this.authClient = authClient;
    }

    @GetMapping("/flights")
    public List<Map<String, Object>> getFlightsForBookingUi() {
        return fetchFlights().stream().map(this::withAirlineName).toList();
    }

    @PostMapping("/api/v1/flights/search")
    public List<Map<String, Object>> searchFlights(@RequestBody Map<String, Object> criteria) {
        String from = str(criteria.get("fromLocation"));
        String to = str(criteria.get("toLocation"));
        String date = str(criteria.get("departureDate"));
        String type = str(criteria.get("flightType")).toLowerCase(Locale.ROOT);

        return fetchFlights().stream()
                .filter(f -> from.isBlank() || from.equalsIgnoreCase(str(f.get("origin"))))
                .filter(f -> to.isBlank() || to.equalsIgnoreCase(str(f.get("destination"))))
                .filter(f -> date.isBlank() || departureDateOf(str(f.get("departureTime"))).equals(date))
                .filter(f -> type.isBlank() || routeMatchesType(type, str(f.get("origin")), str(f.get("destination"))))
                .map(this::withAirlineName)
                .toList();
    }

    @PostMapping("/flights/passenger/confirm")
    public Map<String, Object> confirmLegacyBooking(@RequestBody Map<String, Object> payload) {
        Long flightId = asLong(payload.get("flightId"));
        if (flightId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "flightId is required");
        }

        List<Map<String, Object>> passengerDetails = listOfMap(payload.get("passengerDetails"));
        String passengerName = passengerDetails.isEmpty() ? "Guest Passenger" : str(passengerDetails.get(0).get("fullName"));

        Map<String, Object> createBookingPayload = new HashMap<>();
        createBookingPayload.put("flightId", flightId);
        createBookingPayload.put("passengerName", passengerName);
        createBookingPayload.put("seatNumber", str(payload.get("seat")));
        createBookingPayload.put("fareAmount", new BigDecimal(String.valueOf(payload.getOrDefault("fare", 0))));

        Map<String, Object> saved = bookingClient.createBooking(createBookingPayload);

        Long bookingId = asLong(saved == null ? null : saved.get("bookingId"));
        if (bookingId == null || saved == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Booking service response was invalid");
        }

        Map<String, Object> legacy = toLegacyBookingView(saved, payload);
        bookingViewState.put(bookingId, legacy);
        return legacy;
    }

    @GetMapping("/flights/bookings")
    public List<Map<String, Object>> getLegacyBookings() {
        List<Map<String, Object>> list = bookingClient.getBookings();
        if (list == null) {
            return List.of();
        }
        return list.stream().map(this::toLegacyBookingViewFromStored).toList();
    }

    @GetMapping("/flights/confirmation/{bookingId}")
    public Map<String, Object> getLegacyBookingConfirmation(@PathVariable Long bookingId) {
        Map<String, Object> booking = bookingClient.getBooking(bookingId);
        if (booking == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        return toLegacyBookingViewFromStored(booking);
    }

    @PostMapping("/flights/{bookingId}/modify")
    public Map<String, Object> modifyLegacyBooking(@PathVariable Long bookingId, @RequestBody Map<String, Object> payload) {
        Map<String, Object> state = bookingViewState.getOrDefault(bookingId, new HashMap<>());
        Map<String, Object> request = new HashMap<>();
        request.put("passengerName", str(state.getOrDefault("primaryPassengerName", "Guest Passenger")));
        request.put("seatNumber", str(payload.get("seat")));

        Map<String, Object> updated = bookingClient.updateBooking(bookingId, request);
        if (updated == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to modify booking");
        }

        Map<String, Object> merged = new HashMap<>(state);
        merged.put("seat", str(payload.get("seat")));
        merged.put("flyDate", str(payload.get("departureDate")));
        bookingViewState.put(bookingId, merged);
        return toLegacyBookingView(updated, merged);
    }

    @PostMapping("/flights/{bookingId}/cancel")
    public Map<String, Object> cancelLegacyBooking(@PathVariable Long bookingId) {
        Map<String, Object> cancelled = bookingClient.cancelBooking(bookingId);
        if (cancelled == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to cancel booking");
        }
        return toLegacyBookingViewFromStored(cancelled);
    }

    @GetMapping("/airline/api/seats")
    public Map<String, Object> getSeatInventory(@RequestParam Long flightId) {
        List<Map<String, Object>> allBookings = bookingClient.getBookings();

        Set<String> booked = (allBookings == null ? List.<Map<String, Object>>of() : allBookings).stream()
                .filter(b -> Objects.equals(asLong(b.get("flightId")), flightId))
                .filter(b -> !"CANCELLED".equalsIgnoreCase(str(b.get("bookingStatus"))))
                .map(b -> str(b.get("seatNumber")))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        List<Map<String, Object>> allSeats = new ArrayList<>();
        String[] rows = {"A", "B", "C", "D", "E", "F"};
        for (String row : rows) {
            for (int col = 1; col <= 4; col++) {
                String seat = row + col;
                Map<String, Object> m = new HashMap<>();
                m.put("seatNumber", seat);
                m.put("seatStatus", booked.contains(seat) ? "BOOKED" : "AVAILABLE");
                allSeats.add(m);
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("bookedSeatNumbers", booked);
        body.put("flight", Map.of("seatAisleAfter", 2));
        body.put("allSeats", allSeats);
        return body;
    }

    @GetMapping("/api/crew/roster/{crewMemberName}")
    public List<Map<String, Object>> getCrewRosterByName(@PathVariable String crewMemberName) {
        List<Map<String, Object>> roster = crewClient.getCrewRoster();
        if (roster == null) {
            return List.of();
        }
        return roster.stream()
                .filter(r -> crewMemberName.equalsIgnoreCase(str(r.get("crewMemberName"))))
                .toList();
    }

    @PutMapping("/api/crew/swap/{assignmentId}")
    public Map<String, Object> swapCrewLegacy(@PathVariable Long assignmentId, @RequestBody Map<String, Object> payload) {
        String newCrewMemberName = str(payload.get("newCrewMemberName"));
        return crewClient.swapCrew(assignmentId, Map.of("crewMemberName", newCrewMemberName));
    }

    @GetMapping("/api/loyalty/member/{memberId}")
    public Map<String, Object> getLoyaltyMemberLegacy(@PathVariable Long memberId) {
        return loyaltyClient.getLoyaltyMember(memberId);
    }

    @GetMapping("/api/loyalty/members")
    public List<Map<String, Object>> getLoyaltyMembersLegacy() {
        List<Map<String, Object>> members = loyaltyClient.getLoyaltyMembers();
        return members == null ? List.of() : members;
    }

    @PostMapping("/api/loyalty/credit")
    public Map<String, Object> creditLoyaltyLegacy(@RequestParam Long memberId, @RequestParam int miles) {
        return loyaltyClient.creditLoyalty(memberId, Map.of("miles", miles));
    }

    @PostMapping("/api/loyalty/redeem")
    public Map<String, Object> redeemLoyaltyLegacy(@RequestParam Long memberId, @RequestParam int miles) {
        return loyaltyClient.redeemLoyalty(memberId, Map.of("miles", miles));
    }

    @GetMapping("/api/v1/flights")
    public List<Map<String, Object>> getFlightsForSchedulerUi() {
        return fetchFlights().stream().map(this::toSchedulerFlightView).toList();
    }

    @GetMapping("/api/v1/flights/metadata")
    public Map<String, Object> getFlightMetadata() {
        List<Map<String, String>> indian = List.of(
                airport("MAA", "Chennai"), airport("BOM", "Mumbai"), airport("BLR", "Bangalore"),
                airport("DEL", "Delhi"), airport("HYD", "Hyderabad"), airport("CJB", "Coimbatore"),
                airport("CCU", "Kolkata"), airport("PNQ", "Pune")
        );
        List<Map<String, String>> intl = List.of(
                airport("DXB", "Dubai"), airport("SIN", "Singapore"), airport("LHR", "London"),
                airport("JFK", "New York"), airport("BKK", "Bangkok")
        );
        return Map.of(
                "indianAirports", indian,
                "internationalAirports", intl,
                "statuses", List.of("SCHEDULED", "BOARDING", "DEPARTED", "ARRIVED", "CANCELLED")
        );
    }

    @PostMapping("/api/v1/flights")
    public Map<String, Object> createFlightLegacy(@RequestBody Map<String, Object> payload) {
        Map<String, Object> request = new HashMap<>();
        request.put("flightNumber", str(payload.get("flightNumber")));
        request.put("origin", str(payload.get("origin")));
        request.put("destination", str(payload.get("destination")));
        request.put("departureTime", normalizeDateTime(str(payload.get("departureTime"))));
        request.put("arrivalTime", normalizeDateTime(str(payload.get("arrivalTime"))));
        request.put("flightStatus", "SCHEDULED");
        request.put("economyFare", asBigDecimal(payload.get("economyFare")));
        request.put("businessFare", asBigDecimal(payload.get("premiumFare")));
        request.put("firstFare", asBigDecimal(payload.get("firstFare")));

        Map<String, Object> saved = flightClient.createFlight(request);
        if (saved == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to create flight");
        }
        return toSchedulerFlightView(saved);
    }

    @PutMapping("/api/v1/flights/{flightNumber}/schedule")
    public Map<String, Object> updateFlightScheduleLegacy(@PathVariable String flightNumber, @RequestBody Map<String, Object> payload) {
        Map<String, Object> flight = findFlightByNumber(flightNumber);
        Long flightId = asLong(flight.get("flightId"));
        if (flightId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight not found");
        }

        Map<String, Object> updated = flight;

        String departure = normalizeDateTime(str(payload.get("departureTime")));
        String arrival = normalizeDateTime(str(payload.get("arrivalTime")));
        if (departure != null && arrival != null) {
            Map<String, Object> scheduleReq = new HashMap<>();
            scheduleReq.put("departureTime", departure);
            scheduleReq.put("arrivalTime", arrival);

            updated = flightClient.updateFlightSchedule(flightId, scheduleReq);
        }

        String status = str(payload.get("flightStatus"));
        if (!status.isBlank()) {
            updated = flightClient.updateFlightStatus(flightId, Map.of("flightStatus", status));
        }

        return updated == null ? Map.of() : toSchedulerFlightView(updated);
    }

    @PutMapping("/api/v1/flights/{flightNumber}/fares")
    public Map<String, Object> updateFlightFaresLegacy(@PathVariable String flightNumber, @RequestBody Map<String, Object> payload) {
        Map<String, Object> flight = findFlightByNumber(flightNumber);
        Long flightId = asLong(flight.get("flightId"));
        if (flightId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight not found");
        }

        Map<String, Object> req = new HashMap<>();
        req.put("economyFare", asBigDecimal(payload.get("economyFare")));
        req.put("businessFare", asBigDecimal(payload.get("premiumFare")));
        req.put("firstFare", asBigDecimal(payload.get("firstFare")));

        Map<String, Object> updated = flightClient.updateFlightFares(flightId, req);
        return updated == null ? Map.of() : toSchedulerFlightView(updated);
    }

    @DeleteMapping("/api/v1/flights/{flightNumber}")
    public ResponseEntity<Void> deleteFlightLegacy(@PathVariable String flightNumber) {
        Map<String, Object> flight = findFlightByNumber(flightNumber);
        Long flightId = asLong(flight.get("flightId"));
        if (flightId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight not found");
        }
        flightClient.deleteFlight(flightId);
        return ResponseEntity.noContent().build();
    }

    private Map<String, String> airport(String code, String city) {
        return Map.of("code", code, "city", city);
    }

    private boolean routeMatchesType(String type, String origin, String destination) {
        boolean international = INTERNATIONAL_AIRPORTS.contains(origin) || INTERNATIONAL_AIRPORTS.contains(destination);
        if ("international".equals(type)) {
            return international;
        }
        if ("domestic".equals(type)) {
            return !international;
        }
        return true;
    }

    private String departureDateOf(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replace("T", " ");
        int idx = normalized.indexOf(' ');
        return idx > 0 ? normalized.substring(0, idx) : normalized;
    }

    private String normalizeDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        LocalDateTime dt = value.contains("T")
                ? LocalDateTime.parse(value)
                : LocalDateTime.parse(value.replace(" ", "T"));
        return dt.toString();
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(String.valueOf(value)).setScale(2, RoundingMode.HALF_UP);
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private List<Map<String, Object>> listOfMap(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().filter(Map.class::isInstance).map(v -> (Map<String, Object>) v).toList();
    }

    private List<Map<String, Object>> fetchFlights() {
        List<Map<String, Object>> flights = flightClient.getFlights();
        return flights == null ? List.of() : flights;
    }

    private Map<String, Object> withAirlineName(Map<String, Object> flight) {
        Map<String, Object> out = new HashMap<>(flight);
        out.put("airlineName", airlineNameFor(str(flight.get("flightNumber"))));
        return out;
    }

    private String airlineNameFor(String flightNumber) {
        if (flightNumber == null || flightNumber.length() < 2) {
            return flightNumber == null ? "" : flightNumber;
        }
        String prefix = flightNumber.substring(0, 2).toUpperCase(Locale.ROOT);
        return AIRLINE_NAMES.getOrDefault(prefix, flightNumber);
    }

    private Map<String, Object> findFlightByNumber(String flightNumber) {
        return fetchFlights().stream()
                .filter(f -> flightNumber.equalsIgnoreCase(str(f.get("flightNumber"))))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight not found"));
    }

    private Map<String, Object> toLegacyBookingViewFromStored(Map<String, Object> booking) {
        Long id = asLong(booking.get("bookingId"));
        Map<String, Object> fromState = id == null ? null : bookingViewState.get(id);
        return toLegacyBookingView(booking, fromState == null ? Map.of() : fromState);
    }

    private Map<String, Object> toLegacyBookingView(Map<String, Object> booking, Map<String, Object> sourcePayload) {
        Map<String, Object> out = new HashMap<>();
        Long bookingId = asLong(booking.get("bookingId"));
        out.put("id", bookingId);
        out.put("bookingId", bookingId);
        out.put("pnr", str(booking.get("pnr")));
        out.put("status", str(booking.get("bookingStatus")));
        out.put("bookingStatus", str(booking.get("bookingStatus")));
        out.put("seat", str(booking.get("seatNumber")));
        out.put("fare", booking.get("fareAmount"));
        out.put("flightId", booking.get("flightId"));
        out.put("passengers", sourcePayload.getOrDefault("passengers", 1));
        out.put("passengerDetails", sourcePayload.getOrDefault("passengerDetails", List.of()));
        out.put("airline", str(sourcePayload.get("airline")));
        out.put("flightType", str(sourcePayload.get("flightType")));
        out.put("fromLocation", str(sourcePayload.get("fromLocation")));
        out.put("toLocation", str(sourcePayload.get("toLocation")));
        out.put("flyDate", str(sourcePayload.get("departureDate")));
        out.put("departureDate", str(sourcePayload.get("departureDate")));
        out.put("primaryPassengerName", str(booking.get("passengerName")));
        return out;
    }

    private Map<String, Object> toSchedulerFlightView(Map<String, Object> flight) {
        Map<String, Object> out = new HashMap<>();
        out.put("flightId", flight.get("flightId"));
        out.put("flightNumber", str(flight.get("flightNumber")));
        out.put("flightName", str(flight.get("flightNumber")));
        out.put("origin", str(flight.get("origin")));
        out.put("destination", str(flight.get("destination")));

        String departure = str(flight.get("departureTime"));
        String arrival = str(flight.get("arrivalTime"));
        out.put("departureTime", formatLegacyDateTime(departure));
        out.put("arrivalTime", formatLegacyDateTime(arrival));

        out.put("distanceMiles", estimateDistanceMiles(str(flight.get("origin")), str(flight.get("destination"))));
        out.put("economyFare", flight.get("economyFare"));
        out.put("premiumFare", flight.get("businessFare"));
        out.put("firstFare", flight.get("firstFare"));
        out.put("flightStatus", str(flight.get("flightStatus")));
        return out;
    }

    private String formatLegacyDateTime(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            LocalDateTime dt = value.contains("T") ? LocalDateTime.parse(value) : LocalDateTime.parse(value.replace(" ", "T"));
            return DATE_TIME_FMT.format(dt);
        } catch (Exception ex) {
            return value;
        }
    }

    private double estimateDistanceMiles(String origin, String destination) {
        if (origin.equals(destination)) {
            return 0;
        }
        if (routeMatchesType("international", origin, destination)) {
            return 2900.0;
        }
        return 820.0;
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<byte[]> login(@RequestBody Map<String, Object> credentials) {
        try {
            return authClient.login(credentials);
        } catch (feign.FeignException ex) {
            org.springframework.http.HttpHeaders responseHeaders = new org.springframework.http.HttpHeaders();
            if (ex.responseHeaders() != null) {
                ex.responseHeaders().forEach((k, v) -> responseHeaders.addAll(k, v.stream().toList()));
            }
            byte[] responseBody = ex.content() != null ? ex.content() : new byte[0];
            return ResponseEntity.status(ex.status()).headers(responseHeaders).body(responseBody);
        }
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<byte[]> register(@RequestBody Map<String, Object> user) {
        try {
            return authClient.register(user);
        } catch (feign.FeignException ex) {
            org.springframework.http.HttpHeaders responseHeaders = new org.springframework.http.HttpHeaders();
            if (ex.responseHeaders() != null) {
                ex.responseHeaders().forEach((k, v) -> responseHeaders.addAll(k, v.stream().toList()));
            }
            byte[] responseBody = ex.content() != null ? ex.content() : new byte[0];
            return ResponseEntity.status(ex.status()).headers(responseHeaders).body(responseBody);
        }
    }
}
