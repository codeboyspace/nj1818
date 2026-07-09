package com.genc.arfoms.checkin.service;

import com.genc.arfoms.checkin.client.BookingClient;
import com.genc.arfoms.checkin.client.FlightClient;
import com.genc.arfoms.checkin.dto.BoardingPassView;
import com.genc.arfoms.checkin.dto.BookingLookupResult;
import com.genc.arfoms.checkin.dto.BookingView;
import com.genc.arfoms.checkin.dto.CheckInDetailsView;
import com.genc.arfoms.checkin.dto.FlightView;
import com.genc.arfoms.checkin.model.CheckIn;
import com.genc.arfoms.checkin.model.CheckInStatus;
import com.genc.arfoms.checkin.repository.CheckInRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final BookingClient bookingClient;
    private final FlightClient flightClient;

    public CheckInService(CheckInRepository checkInRepository,
                          BookingClient bookingClient,
                          FlightClient flightClient) {
        this.checkInRepository = checkInRepository;
        this.bookingClient = bookingClient;
        this.flightClient = flightClient;
    }

    /**
     * Looks up a booking (and its flight) live from the owning services so the
     * agent can confirm the real passenger before checking them in. No data is
     * hardcoded here - everything comes from the booking/flight databases.
     */
    public BookingLookupResult lookupBooking(Long bookingId) {
        BookingView booking = getBookingSafe(bookingId);
        if (booking == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        FlightView flight = getFlightSafe(booking.flightId());
        Optional<CheckIn> existing = checkInRepository.findFirstByBookingId(bookingId);
        return new BookingLookupResult(
                booking,
                flight,
                existing.isPresent(),
                existing.map(CheckIn::getCheckInId).orElse(null));
    }

    public CheckIn checkInPassenger(CheckIn checkIn) {
        BookingView booking = getBookingSafe(checkIn.getBookingId());
        if (booking == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking does not exist");
        }
        if ("CANCELLED".equalsIgnoreCase(booking.bookingStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot check in a cancelled booking");
        }
        if (checkInRepository.existsByBookingId(booking.bookingId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Passenger already checked in for this booking");
        }
        checkIn.setBookingId(booking.bookingId());
        checkIn.setCheckInTime(LocalDateTime.now());
        checkIn.setCheckInStatus(CheckInStatus.CHECKED_IN);
        return checkInRepository.save(checkIn);
    }

    public CheckIn boardPassenger(Long checkInId) {
        CheckIn checkIn = getCheckIn(checkInId);
        if (checkIn.getCheckInStatus() != CheckInStatus.CHECKED_IN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only checked-in passengers can be boarded");
        }
        checkIn.setCheckInStatus(CheckInStatus.BOARDED);
        return checkInRepository.save(checkIn);
    }

    /**
     * Issues a boarding pass for an already checked-in passenger. The pass is
     * built from the persisted check-in plus the live booking + flight details,
     * so every field reflects real data.
     */
    public BoardingPassView issueBoardingPass(Long checkInId) {
        CheckIn checkIn = getCheckIn(checkInId);
        if (checkIn.getCheckInStatus() == CheckInStatus.OFFLOADED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot issue a boarding pass for an offloaded passenger");
        }
        BookingView booking = getBookingSafe(checkIn.getBookingId());
        if (booking == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking no longer exists");
        }
        FlightView flight = getFlightSafe(booking.flightId());
        return new BoardingPassView(
                checkIn.getCheckInId(),
                booking.bookingId(),
                booking.pnr(),
                booking.passengerName(),
                booking.seatNumber(),
                flight != null ? flight.flightNumber() : null,
                flight != null ? flight.origin() : null,
                flight != null ? flight.destination() : null,
                flight != null ? flight.departureTime() : null,
                checkIn.getCheckInStatus() != null ? checkIn.getCheckInStatus().name() : null,
                checkIn.getCheckInId(),
                LocalDateTime.now());
    }

    public CheckIn tagBaggage(Long checkInId, int bagCount) {
        CheckIn checkIn = getCheckIn(checkInId);
        checkIn.setBaggageCount(bagCount);
        return checkInRepository.save(checkIn);
    }

    public CheckIn getCheckIn(Long checkInId) {
        return checkInRepository.findById(checkInId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Check-in not found"));
    }

    public List<CheckIn> getAll() {
        return checkInRepository.findAll();
    }

    /**
     * Returns every check-in enriched with the live passenger + flight context
     * so the operations registry shows real names/routes from the database.
     */
    public List<CheckInDetailsView> getAllDetails() {
        return checkInRepository.findAll().stream()
                .map(this::toDetails)
                .toList();
    }

    private CheckInDetailsView toDetails(CheckIn checkIn) {
        BookingView booking = getBookingSafe(checkIn.getBookingId());
        FlightView flight = booking != null ? getFlightSafe(booking.flightId()) : null;
        return new CheckInDetailsView(
                checkIn.getCheckInId(),
                checkIn.getBookingId(),
                booking != null ? booking.pnr() : null,
                booking != null ? booking.passengerName() : null,
                booking != null ? booking.seatNumber() : null,
                flight != null ? flight.flightId() : (booking != null ? booking.flightId() : null),
                flight != null ? flight.flightNumber() : null,
                flight != null ? flight.origin() : null,
                flight != null ? flight.destination() : null,
                checkIn.getCheckInStatus() != null ? checkIn.getCheckInStatus().name() : null,
                checkIn.getBaggageCount(),
                checkIn.getBaggageWeight(),
                checkIn.getCheckInTime());
    }
    private BookingView getBookingSafe(Long bookingId) {
        if (bookingId == null) return null;
        try {
            return bookingClient.getBooking(bookingId);
        } catch (feign.FeignException.NotFound e) {
            return null;
        }
    }

    private FlightView getFlightSafe(Long flightId) {
        if (flightId == null) return null;
        try {
            return flightClient.getFlight(flightId);
        } catch (feign.FeignException.NotFound e) {
            return null;
        }
    }
}

