package com.genc.arfoms1.model;

import com.genc.arfoms1.model.enums.SeatStatus;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seat_inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_id", nullable = false)
    private Integer flightId;

    @Column(name = "seat_number", length = 10, nullable = false)
    private String seatNumber;

    @Column(name = "column_letter", length = 5, nullable = false)
    private String columnLetter;

    @Column(name = "seat_row", nullable = false)
    private Integer seatRow;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_status", length = 20, nullable = false)
    private SeatStatus seatStatus = SeatStatus.AVAILABLE;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}

