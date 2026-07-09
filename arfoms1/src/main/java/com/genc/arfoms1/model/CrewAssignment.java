package com.genc.arfoms1.model;

import com.genc.arfoms1.model.enums.AssignmentStatus;
import com.genc.arfoms1.model.enums.CrewRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "CrewAssignment")
public class CrewAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignmentId")
    private Integer assignmentId;

    @NotBlank(message = "Crew member name cannot be blank.")
    @Size(max = 100, message = "Crew member name must not exceed 100 characters.")
    @Column(name = "crewMemberName", nullable = false, length = 100)
    private String crewMemberName;

    @NotNull(message = "Flight ID mapping is required.")
    @Min(value = 1, message = "Flight ID must be a valid positive identifier.")
    @Column(name = "flightId", nullable = false)
    private Integer flightId;

    @NotNull(message = "Operational role is required.")
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private CrewRole role;

    @NotNull(message = "Expected duty hours are required.")
    @DecimalMin(value = "0.0", message = "Duty hours cannot be negative.")
    @DecimalMax(value = "24.0", message = "Duty hours cannot exceed 24 hours.")
    @Column(name = "dutyHours", precision = 4, scale = 1)
    private BigDecimal dutyHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignmentStatus", nullable = false)
    private AssignmentStatus assignmentStatus;


    public CrewAssignment() {
    }

}