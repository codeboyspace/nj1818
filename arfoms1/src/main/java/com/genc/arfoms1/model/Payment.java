package com.genc.arfoms1.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Integer bookingId;

    @Column(name = "base_fare")
    private String baseFare;
    @Column(name = "seat_charges")
    private String seatCharges;
    @Column(name = "taxes")
    private String taxes;
    @Column(name = "total_amount")
    private String totalAmount;
    @Column(name = "savings")
    private String savings;
    @Column(name = "flight_number")
    private String flightNumber;
    @Column(name = "source")
    private String source;
    @Column(name = "destination")
    private String destination;
    @Column(name = "departure_time")
    private String departureTime;
    @Column(name = "arrival_time")
    private String arrivalTime;
    @Column(name = "passenger_name")
    private String passengerName;

    @Column(name = "flight_id")
    private Integer flightId;

    @Column(name = "seat_number", length = 10)
    private String seatNumber;
}
