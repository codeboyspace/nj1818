package com.genc.arfoms1.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "FrequentFlyer")
public class FrequentFlyer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memberId")
    private Integer memberId;

    @Column(name = "memberName", nullable = false, length = 100)
    private String memberName;

    @Column(name = "milesBalance")
    private Integer milesBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "membershipTier")
    private MembershipTier membershipTier;

    @Column(name = "enrollmentDate")
    private LocalDate enrollmentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "memberStatus")
    private MemberStatus memberStatus;

    public enum MembershipTier { SILVER, GOLD, PLATINUM }
    public enum MemberStatus { ACTIVE, INACTIVE }
}
