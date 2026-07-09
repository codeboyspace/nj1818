package com.genc.arfoms1.service;

import com.genc.arfoms1.model.FrequentFlyer;
import com.genc.arfoms1.model.FrequentFlyer.*;
import com.genc.arfoms1.repository.FrequentFlyerRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class LoyaltyService {
    @Autowired
    private FrequentFlyerRepository repository;

    public FrequentFlyer enrollFrequentFlyer(String name) {
        FrequentFlyer member = new FrequentFlyer();
        member.setMemberName(name);
        member.setMilesBalance(0);
        member.setMembershipTier(MembershipTier.SILVER);
        member.setEnrollmentDate(LocalDate.now());
        member.setMemberStatus(MemberStatus.ACTIVE);
        return repository.save(member);
    }

    public FrequentFlyer creditMiles(Integer memberId, int milesToCredit) {
        FrequentFlyer member = repository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (member.getMemberStatus() == MemberStatus.INACTIVE) {
            throw new IllegalStateException("Cannot credit miles to an inactive account");
        }

        member.setMilesBalance(member.getMilesBalance() + milesToCredit);
        updateTier(member);

        return repository.save(member);
    }

    @Transactional
    public FrequentFlyer redeemMiles(Integer memberId, int milesToRedeem) {
        FrequentFlyer member = repository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (member.getMilesBalance() < milesToRedeem) {
            throw new IllegalArgumentException("Insufficient miles balance");
        }

        member.setMilesBalance(member.getMilesBalance() - milesToRedeem);

        updateTier(member);

        return repository.save(member);
    }

    private void updateTier(FrequentFlyer member) {
        int currentMiles = member.getMilesBalance();
        if (currentMiles >= 50000) {
            member.setMembershipTier(MembershipTier.PLATINUM);
        } else if (currentMiles >= 25000) {
            member.setMembershipTier(MembershipTier.GOLD);
        } else {
            member.setMembershipTier(MembershipTier.SILVER);
        }
    }

    public FrequentFlyer getMemberById(Integer memberId) {
        return repository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member ID #" + memberId + " not found."));
    }

    public List<FrequentFlyer> getAllMembers() {
        return repository.findAll();
    }
}
