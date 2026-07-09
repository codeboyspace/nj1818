package com.genc.arfoms.auth;

import com.genc.arfoms.auth.model.User;
import com.genc.arfoms.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username and password required"));
        }

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            User user = userOpt.get();
            String token = jwtUtils.generateToken(username, user.getRole());
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "role", user.getRole(),
                    "name", user.getName()
            ));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid credentials"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User newUser) {
        if (newUser.getUsername() == null || newUser.getPassword() == null || newUser.getRole() == null || newUser.getName() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "All fields are required (username, password, name, role)"));
        }

        if (userRepository.findByUsername(newUser.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Username already exists"));
        }

        // Standardize roles for consistency in the frontend mapping
        String role = newUser.getRole().toLowerCase().trim();
        switch (role) {
            case "admin":
            case "reservation agent":
            case "ground staff":
            case "flight dispatcher":
            case "crew scheduler":
            case "loyalty manager":
                newUser.setRole(role);
                break;
            default:
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid role specified"));
        }

        User savedUser = userRepository.save(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "User registered successfully",
                "userId", savedUser.getId()
        ));
    }

    @GetMapping("/validate")
    public String validateToken(@RequestParam("token") String token) {
        jwtUtils.validateToken(token);
        return "Token is valid";
    }
}
