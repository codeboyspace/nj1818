package com.genc.arfoms.crew.controller;

import com.genc.arfoms.crew.model.CrewAssignment;
import com.genc.arfoms.crew.service.CrewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
@CrossOrigin("*")
@RestController
@RequestMapping("/api/crew")
public class CrewController {

    private final CrewService crewService;

    public CrewController(CrewService crewService) {
        this.crewService = crewService;
    }

    @PostMapping("/assign")
    public CrewAssignment assignCrew(@Valid @RequestBody CrewAssignment assignment) {
        return crewService.assignCrew(assignment);
    }

    @PatchMapping("/{assignmentId}/swap")
    public CrewAssignment swapCrew(@PathVariable Long assignmentId, @RequestBody SwapCrewRequest request) {
        return crewService.swapCrew(assignmentId, request.crewMemberName());
    }

    @PatchMapping("/{assignmentId}/duty-hours")
    public CrewAssignment recordDutyHours(@PathVariable Long assignmentId, @RequestBody DutyHoursRequest request) {
        return crewService.recordDutyHours(assignmentId, request.dutyHours());
    }

    @GetMapping("/{assignmentId}")
    public CrewAssignment getAssignment(@PathVariable Long assignmentId) {
        return crewService.getAssignment(assignmentId);
    }

    @GetMapping("/roster")
    public List<CrewAssignment> getCrewRoster() {
        return crewService.getCrewRoster();
    }

    @GetMapping("/roster/{crewMemberName}")
    public List<CrewAssignment> getCrewRosterByName(@PathVariable String crewMemberName) {
        return crewService.getCrewRoster(crewMemberName);
    }

    public record SwapCrewRequest(String crewMemberName) {
    }

    public record DutyHoursRequest(BigDecimal dutyHours) {
    }
}

