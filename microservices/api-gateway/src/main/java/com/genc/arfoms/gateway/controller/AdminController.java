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
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminUserService users;
    private final RestClient flightClient;
    private final RestClient bookingClient;
    private final RestClient checkinClient;
    private final RestClient crewClient;
    private final RestClient loyaltyClient;

    public AdminController(
            AdminUserService users,
            RestClient.Builder restClientBuilder,
            @Value("${gateway.flight-service.base-url}") String flightBaseUrl,
            @Value("${gateway.booking-service.base-url}") String bookingBaseUrl,
            @Value("${gateway.checkin-service.base-url}") String checkinBaseUrl,
            @Value("${gateway.crew-service.base-url}") String crewBaseUrl,
            @Value("${gateway.loyalty-service.base-url}") String loyaltyBaseUrl) {
        this.users = users;
        this.flightClient = restClientBuilder.clone().baseUrl(flightBaseUrl).build();
        this.bookingClient = restClientBuilder.clone().baseUrl(bookingBaseUrl).build();
        this.checkinClient = restClientBuilder.clone().baseUrl(checkinBaseUrl).build();
        this.crewClient = restClientBuilder.clone().baseUrl(crewBaseUrl).build();
        this.loyaltyClient = restClientBuilder.clone().baseUrl(loyaltyBaseUrl).build();
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
        list.add(probe("flight", "Flight Service", "flights.html", flightClient, "/api/flights"));
        list.add(probe("booking", "Booking Service", "manage-booking.html", bookingClient, "/api/bookings"));
        list.add(probe("checkin", "Check-In Service", "checkin-operations.html", checkinClient, "/api/checkin"));
        list.add(probe("crew", "Crew Service", "crew_roster_management.html", crewClient, "/api/crew/roster"));
        list.add(probe("loyalty", "Loyalty Service", "loyalty_admin_portal.html", loyaltyClient, "/api/loyalty"));
        return list;
    }

    private ModuleStatus probe(String key, String label, String page, RestClient client, String path) {
        try {
            List<?> body = client.get().uri(path).retrieve().body(List.class);
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



