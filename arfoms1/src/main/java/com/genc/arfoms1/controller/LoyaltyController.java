package com.genc.arfoms1.controller;

import com.genc.arfoms1.model.FrequentFlyer;
import com.genc.arfoms1.service.LoyaltyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loyalty")
@CrossOrigin(origins = "*")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    public LoyaltyController(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    @PostMapping("/enroll")
    public ResponseEntity<FrequentFlyer> enrollFrequentFlyer(@RequestParam String name) {
        return ResponseEntity.ok(loyaltyService.enrollFrequentFlyer(name));
    }

    @PostMapping("/credit")
    public ResponseEntity<FrequentFlyer> creditMiles(@RequestParam Integer memberId, @RequestParam int miles) {
        try {
            return ResponseEntity.ok(loyaltyService.creditMiles(memberId, miles));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/redeem")
    public ResponseEntity<FrequentFlyer> redeemMiles(@RequestParam Integer memberId, @RequestParam int miles) {
        try {
            return ResponseEntity.ok(loyaltyService.redeemMiles(memberId, miles));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/member/{id}")
    public ResponseEntity<FrequentFlyer> getMember(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(loyaltyService.getMemberById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/members")
    public ResponseEntity<List<FrequentFlyer>> getAllMembers() {
        try {
            return ResponseEntity.ok(loyaltyService.getAllMembers());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}