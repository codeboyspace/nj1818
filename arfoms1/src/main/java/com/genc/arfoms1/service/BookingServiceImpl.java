package com.genc.arfoms1.service;

import com.genc.arfoms1.model.Airline;
import com.genc.arfoms1.model.Booking;
import com.genc.arfoms1.model.Flight;
import com.genc.arfoms1.model.IndianAirports;
import com.genc.arfoms1.model.Passenger;
import com.genc.arfoms1.model.SeatInventory;
import com.genc.arfoms1.model.enums.SeatStatus;
import com.genc.arfoms1.repository.AirlineRepository;
import com.genc.arfoms1.repository.BookingRepository;
import com.genc.arfoms1.repository.FlightRepository;
import com.genc.arfoms1.repository.SeatInventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private static final String BOOKING_NOT_FOUND = "Booking not found for id: ";

    // Maximum number of passengers allowed per booking
    private static final int MAX_PASSENGERS = 9;

    // Indian Standard Time zone, used to determine "today" for upcoming-flight filtering
    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    // Locations that are considered international (names and IATA codes)
    private static final Set<String> INTERNATIONAL_LOCATIONS = Set.of(
            "DUBAI", "SINGAPORE", "LONDON", "NEW YORK", "BANGKOK",
            "DXB", "SIN", "LHR", "JFK", "BKK"
    );

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final AirlineRepository airlineRepository;
    private final SeatInventoryRepository seatInventoryRepository;

    public BookingServiceImpl(BookingRepository bookingRepository, FlightRepository flightRepository,
                              AirlineRepository airlineRepository, SeatInventoryRepository seatInventoryRepository) {
        this.bookingRepository = bookingRepository;
        this.flightRepository = flightRepository;
        this.airlineRepository = airlineRepository;
        this.seatInventoryRepository = seatInventoryRepository;
    }
    @Override
    public List<Flight> searchAvailableFlights(Booking searchCriteria) {
        String fromCanonical = canonicalLocation(searchCriteria.getFromLocation());
        String toCanonical = canonicalLocation(searchCriteria.getToLocation());
        LocalDate departureDate = parseDate(searchCriteria.getDepartureDate());
        LocalDate todayIst = LocalDate.now(IST_ZONE);

        // Match in Java so the search is tolerant of airport codes vs city names
        // (on both the stored value and the query) and of the string departure time.
        List<Flight> flights = flightRepository.findAll().stream()
                .filter(f -> matchesLocation(f.getOrigin(), fromCanonical))
                .filter(f -> matchesLocation(f.getDestination(), toCanonical))
                .filter(f -> {
                    LocalDate dep = extractDate(f.getDepartureTime());
                    if (departureDate != null) {
                        return dep != null && dep.equals(departureDate);
                    }
                    // No date chosen: show flights departing today (IST) or later
                    return dep == null || !dep.isBefore(todayIst);
                })
                .collect(Collectors.toList());

        // Deduplicate by flight number, keeping the lowest flightId
        flights = new ArrayList<>(flights.stream()
                .collect(Collectors.toMap(
                    Flight::getFlightNumber, f -> f,
                    (a, b) -> a.getFlightId() <= b.getFlightId() ? a : b,
                        LinkedHashMap::new
                )).values()
        );

        // Filter by flight type: domestic shows only domestic routes, international only international
        String flightType = searchCriteria.getFlightType();
        if (flightType != null && !flightType.isBlank()) {
            boolean wantInternational = "international".equalsIgnoreCase(flightType);
            flights = flights.stream()
                    .filter(f -> isInternationalFlight(f) == wantInternational)
                    .collect(Collectors.toList());
        }

        Map<Integer, String> airlineMap = airlineRepository.findAll()
                .stream().collect(Collectors.toMap(Airline::getAirlineId, Airline::getAirlineName));
        flights.forEach(f -> f.setAirlineName(resolveAirlineName(f, airlineMap)));
        log.debug("searchAvailableFlights returning {} flight(s)", flights.size());
        return flights;
    }

    /**
     * Resolves a display name for a flight's carrier. Prefers the mapped airline
     * name (when the flight has an airlineId), then falls back to the flight's
     * declared name, and only shows "Unknown" when neither is available.
     */
    private static String resolveAirlineName(Flight flight, Map<Integer, String> airlineMap) {
        if (flight.getAirlineId() != null) {
            String mapped = airlineMap.get(flight.getAirlineId());
            if (mapped != null && !mapped.isBlank()) {
                return mapped;
            }
        }
        if (flight.getFlightName() != null && !flight.getFlightName().isBlank()) {
            return flight.getFlightName();
        }
        return "Unknown";
    }

    // Maps every known airport code AND city name (upper-cased) to its airport code,
    // so a search works whether the stored/queried value is a code or a city name.
    private static final Map<String, String> LOCATION_TO_CODE = buildLocationToCode();

    private static Map<String, String> buildLocationToCode() {
        Map<String, String> map = new HashMap<>();
        for (IndianAirports.Airport airport : IndianAirports.AIRPORTS) {
            String code = airport.code().toUpperCase(Locale.ROOT);
            map.put(code, code);
            map.put(airport.city().toUpperCase(Locale.ROOT), code);
        }
        // International airports offered by the search page
        putLocation(map, "DXB", "DUBAI");
        putLocation(map, "SIN", "SINGAPORE");
        putLocation(map, "LHR", "LONDON");
        putLocation(map, "JFK", "NEW YORK");
        putLocation(map, "BKK", "BANGKOK");
        return map;
    }

    private static void putLocation(Map<String, String> map, String code, String city) {
        map.put(code, code);
        map.put(city, code);
    }

    // Canonicalizes a location (code or city name) to its airport code for comparison.
    private static String canonicalLocation(String location) {
        if (location == null) {
            return null;
        }
        String key = location.trim().toUpperCase(Locale.ROOT);
        if (key.isEmpty()) {
            return null;
        }
        return LOCATION_TO_CODE.getOrDefault(key, key);
    }

    // True when no filter is requested, or the stored location matches the query.
    private static boolean matchesLocation(String stored, String queryCanonical) {
        if (queryCanonical == null) {
            return true;
        }
        return queryCanonical.equals(canonicalLocation(stored));
    }

    // Extracts the date part from a stored "yyyy-MM-dd HH:mm" / "yyyy-MM-ddTHH:mm" value.
    private static LocalDate extractDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String datePart = value.trim().replace('T', ' ').split(" ")[0];
        try {
            return LocalDate.parse(datePart);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }


    @Override
    public Booking prepareBookingDraft(Booking booking) {
        Booking bookingDraft = booking == null ? new Booking() : booking;
        int passengerCount = clampPassengers(bookingDraft.getPassengers());
        bookingDraft.setPassengers(passengerCount);
        ensurePassengerSlots(bookingDraft, passengerCount);
        return bookingDraft;
    }

    @Override
    @Transactional
    public Booking createBooking(Booking booking) {
        Booking bookingToSave = booking == null ? new Booking() : booking;

        int passengerCount = clampPassengers(bookingToSave.getPassengers());
        bookingToSave.setPassengers(passengerCount);
        ensurePassengerSlots(bookingToSave, passengerCount);

        if (bookingToSave.getFlightType() == null || bookingToSave.getFlightType().isBlank()) {
            bookingToSave.setFlightType("domestic");
        }
        if (bookingToSave.getStatus() == null || bookingToSave.getStatus().isBlank()) {
            bookingToSave.setStatus("CONFIRMED");
        }
        // Always allocate a fresh, unique PNR for every new booking
        bookingToSave.setPnr(generatePnr());
        if (bookingToSave.getSeat() == null || bookingToSave.getSeat().isBlank()) {
            bookingToSave.setSeat("12B");
        }

        if (bookingToSave.getPassengers() <= 0) {
            bookingToSave.setPassengers(1);
        }

        if (bookingToSave.getDepartureDate() != null && !bookingToSave.getDepartureDate().isBlank()) {
            bookingToSave.setFlyDate(bookingToSave.getDepartureDate());
        } else if (bookingToSave.getFlyDate() == null || bookingToSave.getFlyDate().isBlank()) {
            bookingToSave.setFlyDate(LocalDate.now().plusDays(7).toString());
        }

        // Apply fare rules
        if (bookingToSave.getFare() <= 0) {
            bookingToSave.setFare(calculateFare(bookingToSave));
        }

        // Reserve the chosen seat (and reject if it is already taken)
        SeatInventory seat = findSeat(bookingToSave.getFlightId(), bookingToSave.getSeat());
        if (seat != null && seat.getSeatStatus() == SeatStatus.BOOKED) {
            throw new IllegalStateException(
                    "Seat " + bookingToSave.getSeat() + " is already booked. Please choose another seat.");
        }

        Booking saved = bookingRepository.save(bookingToSave);

        if (seat != null) {
            seat.setSeatStatus(SeatStatus.BOOKED);
            seatInventoryRepository.save(seat);
            log.info("Marked seat {} on flight {} as BOOKED", seat.getSeatNumber(), seat.getFlightId());
        }

        log.info("Created booking id={} with PNR={}", saved.getId(), saved.getPnr());
        return saved;
    }

    /** Looks up the seat-inventory row for a flight + seat number, if both are present. */
    private SeatInventory findSeat(Integer flightId, String seatNumber) {
        if (flightId == null || seatNumber == null || seatNumber.isBlank()) {
            return null;
        }
        return seatInventoryRepository.findByFlightIdAndSeatNumber(flightId, seatNumber).orElse(null);
    }

    @Override
    public Booking selectSeat(Long bookingId, String seat) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException(BOOKING_NOT_FOUND + bookingId));
        if (seat != null && !seat.isBlank()) {
            booking.setSeat(seat);
        }
        return bookingRepository.save(booking);
    }

    @Override
    public Booking modifyBooking(Long bookingId, Booking updatedBooking) {
        Booking existing = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.warn("modifyBooking failed - {}{}", BOOKING_NOT_FOUND, bookingId);
                    return new IllegalArgumentException(BOOKING_NOT_FOUND + bookingId);
                });

        if (updatedBooking != null) {
            if (updatedBooking.getFlightType() != null && !updatedBooking.getFlightType().isBlank()) {
                existing.setFlightType(updatedBooking.getFlightType());
            }
            if (updatedBooking.getFromLocation() != null && !updatedBooking.getFromLocation().isBlank()) {
                existing.setFromLocation(updatedBooking.getFromLocation());
            }
            if (updatedBooking.getToLocation() != null && !updatedBooking.getToLocation().isBlank()) {
                existing.setToLocation(updatedBooking.getToLocation());
            }
            if (updatedBooking.getAirline() != null && !updatedBooking.getAirline().isBlank()) {
                existing.setAirline(updatedBooking.getAirline());
            }
            if (updatedBooking.getSeat() != null && !updatedBooking.getSeat().isBlank()) {
                existing.setSeat(updatedBooking.getSeat());
            }
            if (updatedBooking.getDepartureDate() != null && !updatedBooking.getDepartureDate().isBlank()) {
                existing.setDepartureDate(updatedBooking.getDepartureDate());
                existing.setFlyDate(updatedBooking.getDepartureDate());
            } else if (updatedBooking.getFlyDate() != null && !updatedBooking.getFlyDate().isBlank()) {
                existing.setFlyDate(updatedBooking.getFlyDate());
            }
            if (updatedBooking.getPassengers() > 0) {
                int updatedCount = clampPassengers(updatedBooking.getPassengers());
                existing.setPassengers(updatedCount);
                ensurePassengerSlots(existing, updatedCount);
            }
        }

        // Re-apply fare rules after modification
        existing.setFare(calculateFare(existing));

        Booking saved = bookingRepository.save(existing);
        log.info("Modified booking id={}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Booking cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.warn("cancelBooking failed - {}{}", BOOKING_NOT_FOUND, bookingId);
                    return new IllegalArgumentException(BOOKING_NOT_FOUND + bookingId);
                });
        booking.setStatus("CANCELLED");
        Booking saved = bookingRepository.save(booking);

        // Release the reserved seat so it can be booked again
        SeatInventory seat = findSeat(booking.getFlightId(), booking.getSeat());
        if (seat != null && seat.getSeatStatus() == SeatStatus.BOOKED) {
            seat.setSeatStatus(SeatStatus.AVAILABLE);
            seatInventoryRepository.save(seat);
            log.info("Released seat {} on flight {}", seat.getSeatNumber(), seat.getFlightId());
        }

        log.info("Cancelled booking id={}", saved.getId());
        return saved;
    }

    @Override
    public Booking getBookingDetails() {
        return bookingRepository.findTopByOrderByIdDesc().orElseGet(this::createDefaultBookingPreview);
    }

    @Override
    public Booking getBookingById(Long bookingId) {
        if (bookingId == null) {
            return createDefaultBookingPreview();
        }
        return bookingRepository.findById(bookingId).orElseGet(this::createDefaultBookingPreview);
    }

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByIdDesc();
    }
//mock data
    private Booking createDefaultBookingPreview()
    {
        Booking booking = new Booking();
        booking.setFlightType("domestic");
        booking.setFromLocation("CJB");
        booking.setToLocation("HYD");
        booking.setPassengers(1);
        booking.setPnr("IN773B");
        booking.setStatus("CONFIRMED");
        booking.setAirline("IndiGo");
        booking.setSeat("12B");
        booking.setFare(5200.0);
        booking.setFlyDate("2026-06-15");
        return booking;
    }

    private String generatePnr() {
        return "PNR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private double calculateFare(Booking booking) {
        double baseFare = "international".equalsIgnoreCase(booking.getFlightType()) ? 18500.0 : 5200.0;
        int passengers = Math.max(booking.getPassengers(), 1);
        return baseFare * passengers;
    }

    private boolean isInternationalLocation(String location) {
        return location != null && INTERNATIONAL_LOCATIONS.contains(location.trim().toUpperCase(Locale.ROOT));
    }
    private boolean isInternationalFlight(Flight flight) {
        return isInternationalLocation(flight.getOrigin()) || isInternationalLocation(flight.getDestination());
    }

    private LocalDate parseDate(String value) {        if (value == null || value.isBlank()) {
            return null;
        }
        try
        {
            return LocalDate.parse(value);
        }
        catch (DateTimeParseException ex)
        {
            return null;
        }
    }

    // Ensures passenger count is at least 1 and never exceeds the allowed maximum (9)
    private int clampPassengers(int requested) {
        return Math.min(Math.max(requested, 1), MAX_PASSENGERS);
    }

    private void ensurePassengerSlots(Booking booking, int passengerCount) {        List<Passenger> currentPassengers = booking.getPassengerDetails();
        if (currentPassengers == null)
        {
            currentPassengers = new ArrayList<>();
        }

        for (int index = currentPassengers.size(); index < passengerCount; index++) {
            currentPassengers.add(new Passenger());
        }

        while (currentPassengers.size() > passengerCount) {
            currentPassengers.remove(currentPassengers.size() - 1);
        }

        booking.setPassengerDetails(currentPassengers);
    }
}
