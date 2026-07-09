package com.genc.arfoms.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret:94a08da1fecbb6e8b46990538c7b50b2a7dc524f22c153833b3d1b700f14d9b4}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
            String username = claims.getSubject();
            
            // Extract role from JWT claims
            String jwtRole = claims.get("role", String.class);
            if (jwtRole == null) {
                jwtRole = "user";
            }
            
            // Map the frontend role to Spring Security ROLE_*
            String mappedRole = "ROLE_USER";
            switch (jwtRole.toLowerCase().trim()) {
                case "admin":
                    mappedRole = "ROLE_ADMIN";
                    break;
                case "flight dispatcher":
                case "flight scheduler":
                    mappedRole = "ROLE_FLIGHT_SCHEDULER";
                    break;
                case "reservation agent":
                    mappedRole = "ROLE_RESERVATION_AGENT";
                    break;
                case "crew scheduler":
                case "crew":
                    mappedRole = "ROLE_CREW";
                    break;
                case "loyalty manager":
                case "loyalty":
                    mappedRole = "ROLE_LOYALTY_MANAGER";
                    break;
                case "ground staff":
                    mappedRole = "ROLE_GROUND_STAFF";
                    break;
            }

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    username, null, Collections.singletonList(new SimpleGrantedAuthority(mappedRole)));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}
