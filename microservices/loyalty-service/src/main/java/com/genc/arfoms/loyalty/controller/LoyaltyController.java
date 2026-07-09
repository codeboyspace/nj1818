package com.genc.arfoms.loyalty.controller;

import com.genc.arfoms.loyalty.client.FlightDistanceClient;
import com.genc.arfoms.loyalty.dto.LoyaltyFlightCreditRequest;
import com.genc.arfoms.loyalty.dto.LoyaltyFlightCreditResult;
import com.genc.arfoms.loyalty.dto.LoyaltyOffersResponse;
import com.genc.arfoms.loyalty.model.FrequentFlyer;
import com.genc.arfoms.loyalty.service.LoyaltyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/loyalty")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;
    private final FlightDistanceClient flightDistanceClient;

    public LoyaltyController(LoyaltyService loyaltyService, FlightDistanceClient flightDistanceClient) {
        this.loyaltyService = loyaltyService;
        this.flightDistanceClient = flightDistanceClient;
    }

    @PostMapping("/enroll")
    public FrequentFlyer enrollFrequentFlyer(@RequestBody FrequentFlyer member) {
        return loyaltyService.enrollFrequentFlyer(member);
    }

    @PatchMapping("/{memberId}/credit")
    public FrequentFlyer creditMiles(@PathVariable Long memberId, @RequestBody MilesRequest request) {
        return loyaltyService.creditMiles(memberId, request.miles());
    }

    @PatchMapping("/{memberId}/redeem")
    public FrequentFlyer redeemMiles(@PathVariable Long memberId, @RequestBody MilesRequest request) {
        return loyaltyService.redeemMiles(memberId, request.miles());
    }

    @PatchMapping("/{memberId}/tier")
    public FrequentFlyer upgradeTier(@PathVariable Long memberId) {
        return loyaltyService.upgradeTier(memberId);
    }

    @GetMapping("/{memberId}")
    public FrequentFlyer getMember(@PathVariable Long memberId) {
        return loyaltyService.getMember(memberId);
    }

    @GetMapping
    public List<FrequentFlyer> getAllMembers() {
        return loyaltyService.getAll();
    }

    // ------------------------------------------------------------------
    // Loyalty Manager portal endpoints (param-based) - additive, the
    // existing endpoints above are left untouched so nothing else changes.
    // ------------------------------------------------------------------

    @GetMapping("/member/{memberId}")
    public FrequentFlyer getMemberForPortal(@PathVariable Long memberId) {
        return loyaltyService.getMember(memberId);
    }

    @GetMapping("/members")
    public List<FrequentFlyer> getMembersForPortal() {
        return loyaltyService.getAll();
    }

    @PostMapping("/credit")
    public FrequentFlyer creditMilesByParam(@RequestParam Long memberId, @RequestParam int miles) {
        return loyaltyService.creditMiles(memberId, miles);
    }

    @PostMapping("/redeem")
    public FrequentFlyer redeemMilesByParam(@RequestParam Long memberId, @RequestParam int miles) {
        return loyaltyService.redeemMiles(memberId, miles);
    }

    @PostMapping("/credit-flight")
    public LoyaltyFlightCreditResult creditForCompletedFlight(@RequestBody LoyaltyFlightCreditRequest request) {
        return loyaltyService.creditMilesForCompletedFlight(
                request.memberId(), request.bookingId(), request.distanceMiles());
    }

    // ------------------------------------------------------------------
    // Distance-based offers: the loyalty manager tailors offers to how far
    // a member has flown (in miles).
    // ------------------------------------------------------------------

    /** Generate offers directly from a known distance in miles. */
    @GetMapping("/offers")
    public LoyaltyOffersResponse getOffers(@RequestParam double distanceMiles,
                                           @RequestParam(required = false) Long memberId) {
        return loyaltyService.generateOffers(memberId, distanceMiles);
    }

    /**
     * Generate offers for a flight: the distance in miles is fetched from the
     * flight-service (which computes it from the airport coordinates) and then
     * used to build the offers.
     */
    @GetMapping("/offers/by-flight")
    public LoyaltyOffersResponse getOffersForFlight(@RequestParam Long flightId,
                                                    @RequestParam(required = false) Long memberId) {
        double distanceMiles = 0;
        try {
            distanceMiles = flightDistanceClient.distanceForFlight(flightId).distanceMiles();
        } catch (feign.FeignException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_GATEWAY, 
                "Flight service returned no distance or failed for flight " + flightId
            );
        }
        return loyaltyService.generateOffers(memberId, distanceMiles);
    }

    public record MilesRequest(int miles) {
    }
}

