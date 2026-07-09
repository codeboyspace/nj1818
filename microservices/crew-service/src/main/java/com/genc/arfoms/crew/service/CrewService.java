package com.genc.arfoms.crew.service;

import com.genc.arfoms.crew.client.FlightClient;
import com.genc.arfoms.crew.model.AssignmentStatus;
import com.genc.arfoms.crew.model.CrewAssignment;
import com.genc.arfoms.crew.repository.CrewAssignmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

@Service
public class CrewService {

    // Individual assignment safety threshold (per single assignment)
    private static final BigDecimal MAX_ASSIGNMENT_HOURS = new BigDecimal("12.0");
    // Global multi-flight fatigue limit (cumulative across scheduled assignments)
    private static final BigDecimal MAX_CUMULATIVE_HOURS = new BigDecimal("40.0");

    private final CrewAssignmentRepository repository;
    private final FlightClient flightClient;

    public CrewService(CrewAssignmentRepository repository, FlightClient flightClient) {
        this.repository = repository;
        this.flightClient = flightClient;
    }

    @Transactional
    public CrewAssignment assignCrew(CrewAssignment assignment) {
        Long flightId = assignment.getFlightId();

        // 1. Cross-module guard: verify the flight exists in flight-service before roster manipulation.
        if (!flightClient.flightExists(flightId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Integration Guard: Flight ID " + flightId + " does not exist in the system.");
        }

        // 2. Per-assignment safety threshold limit.
        BigDecimal incomingHours = assignment.getDutyHours() != null ? assignment.getDutyHours() : BigDecimal.ZERO;
        if (incomingHours.compareTo(MAX_ASSIGNMENT_HOURS) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Duty hours violate safety rest limits (max 12 hours per assignment).");
        }

        // 3. Global multi-flight fatigue constraint (cumulative scheduled hours).
        BigDecimal currentHours = repository.findByCrewMemberName(assignment.getCrewMemberName()).stream()
                .filter(a -> a.getAssignmentStatus() == AssignmentStatus.SCHEDULED)
                .map(a -> a.getDutyHours() != null ? a.getDutyHours() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (currentHours.add(incomingHours).compareTo(MAX_CUMULATIVE_HOURS) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Crew member exceeds maximum allowed cumulative duty hours limit (max 40 hours).");
        }

        // 4. Role duplication safety: prevent double booking of an identical operational slot.
        boolean roleTaken = repository.findByFlightId(flightId).stream()
                .anyMatch(a -> a.getRole() == assignment.getRole()
                        && a.getAssignmentStatus() == AssignmentStatus.SCHEDULED);
        if (roleTaken) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Role " + assignment.getRole() + " is already assigned to Flight ID " + flightId);
        }

        assignment.setAssignmentStatus(AssignmentStatus.SCHEDULED);
        return repository.save(assignment);
    }

    @Transactional
    public CrewAssignment swapCrew(Long assignmentId, String newCrewMemberName) {
        CrewAssignment original = getAssignment(assignmentId);

        original.setAssignmentStatus(AssignmentStatus.SWAPPED);
        repository.save(original);

        CrewAssignment newAssignment = new CrewAssignment();
        newAssignment.setCrewMemberName(newCrewMemberName);
        newAssignment.setFlightId(original.getFlightId());
        newAssignment.setRole(original.getRole());
        newAssignment.setDutyHours(original.getDutyHours());
        newAssignment.setAssignmentStatus(AssignmentStatus.SCHEDULED);
        return repository.save(newAssignment);
    }

    @Transactional
    public CrewAssignment recordDutyHours(Long assignmentId, BigDecimal hours) {
        CrewAssignment assignment = getAssignment(assignmentId);

        try {
            // Derive real operational duration from the flight's departure/arrival window.
            FlightClient.FlightView flight = flightClient.getFlight(assignment.getFlightId());
            if (flight != null && flight.departureTime() != null && flight.arrivalTime() != null) {
                long minutes = Duration.between(flight.departureTime(), flight.arrivalTime()).toMinutes();
                hours = BigDecimal.valueOf(minutes / 60.0).setScale(1, RoundingMode.HALF_UP);
            }
        } catch (Exception ex) {
            // Fallback: use manual user entry if dynamic time tracking fails.
            if (hours == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Dynamic time tracking failed, and no manual hours fallback was provided.");
            }
        }

        if (hours == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No duty hours could be determined for this assignment.");
        }

        assignment.setDutyHours(hours);
        assignment.setAssignmentStatus(AssignmentStatus.COMPLETED);
        return repository.save(assignment);
    }

    @Transactional(readOnly = true)
    public CrewAssignment getAssignment(Long assignmentId) {
        return repository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment ID not found."));
    }

    @Transactional(readOnly = true)
    public List<CrewAssignment> getCrewRoster() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CrewAssignment> getCrewRoster(String crewMemberName) {
        return repository.findByCrewMemberName(crewMemberName);
    }

    @Transactional
    public void deleteCrew(Long assignmentId) {
        if (!repository.existsById(assignmentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Crew assignment not found with id: " + assignmentId);
        }
        repository.deleteById(assignmentId);
    }
}
