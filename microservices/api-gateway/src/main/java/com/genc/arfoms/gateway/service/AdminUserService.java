package com.genc.arfoms.gateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns the gateway's in-memory accounts (one per module role) and lets the Admin
 * change usernames / passwords at runtime.
 *
 * <p>Changes apply immediately to the running gateway. They are NOT persisted, so
 * a gateway restart resets the accounts to the defaults in application.properties.
 */
@Service
public class AdminUserService {

    /** Public view of an account (never exposes the password). */
    public record Account(String key, String displayName, List<String> roles, String username) {
    }

    private static final class AccountState {
        final String displayName;
        final List<String> roles;
        String username;
        String encodedPassword;

        AccountState(String displayName, List<String> roles, String username, String encodedPassword) {
            this.displayName = displayName;
            this.roles = roles;
            this.username = username;
            this.encodedPassword = encodedPassword;
        }
    }

    private final PasswordEncoder encoder;
    private final InMemoryUserDetailsManager manager;
    private final Map<String, AccountState> accounts = new LinkedHashMap<>();

    public AdminUserService(
            PasswordEncoder encoder,
            @Value("${arfoms.security.admin.name:admin}") String adminName,
            @Value("${arfoms.security.admin.password:Admin@123}") String adminPassword,
            @Value("${arfoms.security.scheduler.name:scheduler}") String schedulerName,
            @Value("${arfoms.security.scheduler.password:Scheduler@123}") String schedulerPassword,
            @Value("${arfoms.security.agent.name:agent}") String agentName,
            @Value("${arfoms.security.agent.password:Agent@123}") String agentPassword,
            @Value("${arfoms.security.crew.name:crew}") String crewName,
            @Value("${arfoms.security.crew.password:Crew@123}") String crewPassword,
            @Value("${arfoms.security.loyalty.name:loyalty}") String loyaltyName,
            @Value("${arfoms.security.loyalty.password:Loyalty@123}") String loyaltyPassword,
            @Value("${arfoms.security.groundstaff.name:groundstaff}") String groundStaffName,
            @Value("${arfoms.security.groundstaff.password:Ground@123}") String groundStaffPassword) {

        this.encoder = encoder;

        List<UserDetails> initialUsers = new ArrayList<>();
        define(initialUsers, "admin", "Admin",
                List.of("ADMIN", "FLIGHT_SCHEDULER", "RESERVATION_AGENT", "CREW", "LOYALTY_MANAGER", "GROUND_STAFF"),
                adminName, adminPassword);
        define(initialUsers, "scheduler", "Flight Scheduler", List.of("FLIGHT_SCHEDULER"), schedulerName, schedulerPassword);
        define(initialUsers, "agent", "Reservation Agent", List.of("RESERVATION_AGENT"), agentName, agentPassword);
        define(initialUsers, "crew", "Crew", List.of("CREW"), crewName, crewPassword);
        define(initialUsers, "loyalty", "Loyalty Manager", List.of("LOYALTY_MANAGER"), loyaltyName, loyaltyPassword);
        define(initialUsers, "groundstaff", "Ground Staff", List.of("GROUND_STAFF"), groundStaffName, groundStaffPassword);

        this.manager = new InMemoryUserDetailsManager(initialUsers);
    }

    private void define(List<UserDetails> sink, String key, String displayName, List<String> roles,
                        String username, String rawPassword) {
        String encoded = encoder.encode(rawPassword);
        accounts.put(key, new AccountState(displayName, roles, username, encoded));
        sink.add(User.withUsername(username)
                .password(encoded)
                .authorities(toAuthorities(roles))
                .build());
    }

    private List<GrantedAuthority> toAuthorities(List<String> roles) {
        List<GrantedAuthority> list = new ArrayList<>();
        for (String role : roles) {
            list.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return list;
    }

    public InMemoryUserDetailsManager getManager() {
        return manager;
    }

    public List<Account> listAccounts() {
        List<Account> out = new ArrayList<>();
        accounts.forEach((key, state) -> out.add(new Account(key, state.displayName, state.roles, state.username)));
        return out;
    }

    /**
     * Update an account's username and/or password. A blank value leaves that
     * field unchanged. Returns the updated (password-free) account view.
     */
    public synchronized Account updateAccount(String key, String newUsername, String newPassword) {
        AccountState state = accounts.get(key);
        if (state == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown account: " + key);
        }

        String oldUsername = state.username;
        String finalUsername = (newUsername != null && !newUsername.isBlank())
                ? newUsername.trim()
                : oldUsername;

        boolean usernameChanged = !finalUsername.equals(oldUsername);
        if (usernameChanged && manager.userExists(finalUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already in use: " + finalUsername);
        }

        String encodedPassword = (newPassword != null && !newPassword.isBlank())
                ? encoder.encode(newPassword)
                : state.encodedPassword;

        try {
            UserDetails updated = User.withUsername(finalUsername)
                    .password(encodedPassword)
                    .authorities(toAuthorities(state.roles))
                    .build();

            if (usernameChanged) {
                if (manager.userExists(oldUsername)) {
                    manager.deleteUser(oldUsername);
                }
                manager.createUser(updated);
            } else if (manager.userExists(oldUsername)) {
                manager.updateUser(updated);
            } else {
                manager.createUser(updated);
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update account: " + ex.getMessage(), ex);
        }

        state.username = finalUsername;
        state.encodedPassword = encodedPassword;
        return new Account(key, state.displayName, state.roles, state.username);
    }
}



