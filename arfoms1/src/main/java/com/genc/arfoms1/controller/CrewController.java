package com.genc.arfoms1.controller;

import com.genc.arfoms1.model.CrewAssignment;
import com.genc.arfoms1.service.CrewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/crew")
public class CrewController {

    private final CrewService crewService;

    public CrewController(CrewService crewService) {
        this.crewService = crewService;
    }

    @PostMapping("/assign") // assignCrew() [cite: 86]
    public ResponseEntity<CrewAssignment> assignCrew(@RequestBody CrewAssignment assignment) {
        CrewAssignment saved = crewService.assignCrew(assignment);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/swap/{id}") // swapCrew() [cite: 87]
    public ResponseEntity<CrewAssignment> swapCrew(
            @PathVariable Integer id,
            @RequestBody Map<String, String> payload) {
        String newCrewMemberName = payload.get("newCrewMemberName");
        CrewAssignment updated = crewService.swapCrew(id, newCrewMemberName);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/record-duty/{id}") // recordDutyHours() [cite: 88]
    public ResponseEntity<CrewAssignment> recordDutyHours(
            @PathVariable Integer id,
            @RequestBody Map<String, BigDecimal> payload) {
        BigDecimal hours = payload.get("dutyHours");
        CrewAssignment updated = crewService.recordDutyHours(id, hours);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/roster/{name}") // getCrewRoster() [cite: 89]
    public ResponseEntity<List<CrewAssignment>> getCrewRoster(@PathVariable String name) {
        List<CrewAssignment> roster = crewService.getCrewRoster(name);
        return ResponseEntity.ok(roster);
    }
}