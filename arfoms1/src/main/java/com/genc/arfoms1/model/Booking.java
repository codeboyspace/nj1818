package com.genc.arfoms1.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Data
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "flight_id")
    private Integer flightId;

    @Column(name = "flightType")
    private String flightType;

    @Column(name = "fromLocation")
    private String fromLocation;

    @Column(name = "toLocation")
    private String toLocation;

    @Column(name = "departureDate")
    private String departureDate;

    @Column(name = "passengers")
    private int passengers;

    @Column(name = "pnr")
    private String pnr;

    @Column(name = "status")
    private String status;

    @Column(name = "airline")
    private String airline;

    @Column(name = "seat")
    private String seat;

    @Column(name = "fare")
    private double fare;

    @Column(name = "flyDate")
    private String flyDate;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Passenger> passengerDetails = new ArrayList<>();

    // Custom setter kept (Lombok will not override it) to maintain the bidirectional link
    public void setPassengerDetails(List<Passenger> passengerDetails) {
        List<Passenger> incoming = (passengerDetails == null)
                ? new ArrayList<>()
                : new ArrayList<>(passengerDetails);
        this.passengerDetails.clear();
        incoming.forEach(this::addPassengerDetail);
    }

    public void addPassengerDetail(Passenger passenger) {
        if (passenger == null) {
            return;
        }
        passenger.setBooking(this);
        this.passengerDetails.add(passenger);
    }
}