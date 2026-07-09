package com.genc.arfoms.gateway.controller;

import com.genc.arfoms.gateway.service.AdminUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin-only control plane exposed by the gateway. Access is restricted to the
 * ADMIN role in SecurityConfig.
 *
 * <ul>
 *     <li>GET  /api/admin/accounts        - list every module account</li>
 *     <li>PUT  /api/admin/accounts/{key}  - change a module's username/password</li>
 *     <li>GET  /api/admin/overview        - live status + record count per module</li>
 * </ul>
 */
import com.genc.arfoms.gateway.client.FlightFeignClient;
import com.genc.arfoms.gateway.client.BookingFeignClient;
import com.genc.arfoms.gateway.client.CheckinFeignClient;
import com.genc.arfoms.gateway.client.CrewFeignClient;
import com.genc.arfoms.gateway.client.LoyaltyFeignClient;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminUserService users;
    private final FlightFeignClient flightClient;
    private final BookingFeignClient bookingClient;
    private final CheckinFeignClient checkinClient;
    private final CrewFeignClient crewClient;
    private final LoyaltyFeignClient loyaltyClient;

    public AdminController(
            AdminUserService users,
            FlightFeignClient flightClient,
            BookingFeignClient bookingClient,
            CheckinFeignClient checkinClient,
            CrewFeignClient crewClient,
            LoyaltyFeignClient loyaltyClient) {
        this.users = users;
        this.flightClient = flightClient;
        this.bookingClient = bookingClient;
        this.checkinClient = checkinClient;
        this.crewClient = crewClient;
        this.loyaltyClient = loyaltyClient;
    }

    @GetMapping("/accounts")
    public List<AdminUserService.Account> accounts() {
        return users.listAccounts();
    }

    @PutMapping("/accounts/{key}")
    public AdminUserService.Account updateAccount(@PathVariable("key") String key,
                                                  @RequestBody UpdateAccountRequest request) {
        return users.updateAccount(key, request.username(), request.password());
    }

    @GetMapping("/overview")
    public List<ModuleStatus> overview() {
        List<ModuleStatus> list = new ArrayList<>();
        list.add(probe("flight", "Flight Service", "flights.html", () -> flightClient.getFlights()));
        list.add(probe("booking", "Booking Service", "manage-booking.html", () -> bookingClient.getBookings()));
        list.add(probe("checkin", "Check-In Service", "checkin-operations.html", () -> checkinClient.getCheckins()));
        list.add(probe("crew", "Crew Service", "crew_roster_management.html", () -> crewClient.getCrewRoster()));
        list.add(probe("loyalty", "Loyalty Service", "loyalty_admin_portal.html", () -> loyaltyClient.getLoyaltyMembers()));
        return list;
    }

    private ModuleStatus probe(String key, String label, String page, java.util.function.Supplier<List<?>> supplier) {
        try {
            List<?> body = supplier.get();
            int count = (body == null) ? 0 : body.size();
            return new ModuleStatus(key, label, page, "UP", count);
        } catch (Exception ex) {
            return new ModuleStatus(key, label, page, "DOWN", 0);
        }
    }

    public record UpdateAccountRequest(String username, String password) {
    }

    public record ModuleStatus(String key, String label, String page, String status, int count) {
    }
}



