package com.genc.arfoms.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtils jwtUtils;

    // Hardcoded users for simplicity
    private static final Map<String, String> users = new HashMap<>();
    static {
        users.put("admin", "Admin@123");
        users.put("scheduler", "Scheduler@123");
        users.put("agent", "Agent@123");
        users.put("crew", "Crew@123");
        users.put("loyalty", "Loyalty@123");
        users.put("groundstaff", "Ground@123");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam("username") String username, @RequestParam("password") String password) {
        if (users.containsKey(username) && users.get(username).equals(password)) {
            String token = jwtUtils.generateToken(username, username);
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    @GetMapping("/validate")
    public String validateToken(@RequestParam("token") String token) {
        jwtUtils.validateToken(token);
        return "Token is valid";
    }
}
