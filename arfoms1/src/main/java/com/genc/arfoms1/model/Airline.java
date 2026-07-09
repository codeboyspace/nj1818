package com.genc.arfoms1.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "airline")
@Data
public class Airline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "airlineId")
    private Integer airlineId;

    @Column(name = "airlineName")
    private String airlineName;
}
