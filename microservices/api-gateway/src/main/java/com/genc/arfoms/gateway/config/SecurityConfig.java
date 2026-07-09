package com.genc.arfoms.gateway.config;

import com.genc.arfoms.gateway.service.AdminUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Central Spring Security configuration for the API gateway (the single ingress
 * for all module APIs).
 *
 * <p>When {@code arfoms.security.enabled=true} every module is protected with
 * HTTP Basic authentication and role-based access:
 * <ul>
 *     <li>{@code FLIGHT_SCHEDULER} - /api/flights, /api/v1/flights</li>
 *     <li>{@code RESERVATION_AGENT} - /api/bookings, /flights, /airline/api/seats</li>
 *     <li>{@code CREW} - /api/crew</li>
 *     <li>{@code LOYALTY_MANAGER} - /api/loyalty</li>
 *     <li>{@code GROUND_STAFF} - /api/checkin</li>
 *     <li>{@code ADMIN} - all modules + /api/admin control plane</li>
 * </ul>
 *
 * <p>The accounts themselves are owned by {@link AdminUserService} so the Admin
 * can change usernames/passwords at runtime.
 */
@Configuration
public class SecurityConfig {

    @Value("${arfoms.security.enabled:true}")
    private boolean securityEnabled;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(AdminUserService adminUserService) {
        return adminUserService.getManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // Always-open: landing page, error page and CORS preflight.
                    auth.requestMatchers("/", "/error", "/api/auth/**").permitAll();
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    if (securityEnabled) {
                        // Admin control plane (account management + module overview).
                        auth.requestMatchers("/api/admin/**").hasRole("ADMIN");
                        // Flight scheduling module.
                        auth.requestMatchers("/api/flights/**", "/api/v1/flights/**")
                                .hasAnyRole("FLIGHT_SCHEDULER", "ADMIN", "RESERVATION_AGENT");
                        // Reservation / booking module (incl. legacy frontend booking paths).
                        auth.requestMatchers("/api/bookings/**", "/flights/**", "/airline/api/seats/**")
                                .hasAnyRole("RESERVATION_AGENT", "ADMIN");
                        // Crew module.
                        auth.requestMatchers("/api/crew/**")
                                .hasAnyRole("CREW", "ADMIN");
                        // Loyalty module.
                        auth.requestMatchers("/api/loyalty/**")
                                .hasAnyRole("LOYALTY_MANAGER", "ADMIN");
                        // Check-in / ground operations module.
                        auth.requestMatchers("/api/checkin/**")
                                .hasAnyRole("GROUND_STAFF", "ADMIN");

                        auth.anyRequest().authenticated();
                    } else {
                        auth.anyRequest().permitAll();
                    }
                })
                .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
