package com.genc.arfoms1.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.genc.arfoms1.model.enums.AssignmentStatus;
import com.genc.arfoms1.model.CrewAssignment;
import com.genc.arfoms1.repository.CrewRepository;

@Service
public class CrewService {

    private final CrewRepository crewRepository;

    public CrewService(CrewRepository crewRepository) {
        this.crewRepository = crewRepository;
    }

    @Transactional
    public CrewAssignment assignCrew(CrewAssignment assignment) {
        if (assignment.getDutyHours() != null && assignment.getDutyHours().compareTo(new BigDecimal("12.0")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Duty hours violate safety rest limits (max 12 hours per assignment).");
        }
        assignment.setAssignmentStatus(AssignmentStatus.SCHEDULED);
        return crewRepository.save(assignment);
    }

    @Transactional
    public CrewAssignment swapCrew(Integer assignmentId, String newCrewMemberName) {
        CrewAssignment original = crewRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment ID not found."));
        original.setAssignmentStatus(AssignmentStatus.SWAPPED);
        crewRepository.save(original);


        CrewAssignment newAssignment = new CrewAssignment();
        newAssignment.setCrewMemberName(newCrewMemberName);
        newAssignment.setFlightId(original.getFlightId());
        newAssignment.setRole(original.getRole());
        newAssignment.setDutyHours(original.getDutyHours());
        newAssignment.setAssignmentStatus(AssignmentStatus.SCHEDULED);

        return crewRepository.save(newAssignment);
    }

    @Transactional
    public CrewAssignment recordDutyHours(Integer assignmentId, BigDecimal hours) {
        CrewAssignment assignment = crewRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment ID not found."));

        assignment.setDutyHours(hours);
        assignment.setAssignmentStatus(AssignmentStatus.COMPLETED);
        return crewRepository.save(assignment);
    }

    public List<CrewAssignment> getCrewRoster(String crewMemberName) {
        return crewRepository.findByCrewMemberName(crewMemberName);
    }

    public void deleteCrew(Integer id) {
        if (!crewRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Crew assignment not found with id: " + id);
        }
        crewRepository.deleteById(id);
    }
}
