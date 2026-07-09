package com.genc.arfoms1.model;

import com.genc.arfoms1.model.enums.FlightStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "flights")
@Getter
@Setter
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flight_id")
    private Long flightId;

    @Column(name = "flight_number", nullable = false, unique = true)
    private String flightNumber;

    @Column(name = "flight_name")
    private String flightName;

    @Column(name = "origin")
    private String origin;

    @Column(name = "destination")
    private String destination;

    @Column(name = "distance_miles")
    private Double distanceMiles;

    @Column(name = "departure_time")
    private String departureTime;

    @Column(name = "arrival_time")
    private String arrivalTime;

    @Column(name = "fare")
    private double fare;

    @Column(name = "economy_fare")
    private Double economyFare;

    @Column(name = "premium_fare")
    private Double premiumFare;

    @Column(name = "first_fare")
    private Double firstFare;

    @Enumerated(EnumType.STRING)
    @Column(name = "flight_status")
    private FlightStatus flightStatus = FlightStatus.SCHEDULED;

    @Column(name = "seat_count")
    private Integer seatCount;

    @Column(name = "seat_rows")
    private Integer seatRows;

    @Column(name = "seat_columns")
    private Integer seatColumns;

    @Column(name = "seat_aisle_after")
    private Integer seatAisleAfter;

    // Not persisted: kept for compatibility with other modules
    @Transient
    private Integer airlineId;
    @Transient
    private String airlineName;

    public Flight() {
        this.flightStatus = FlightStatus.SCHEDULED;
    }
}
