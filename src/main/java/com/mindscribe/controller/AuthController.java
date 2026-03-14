package com.mindscribe.controller;

import com.mindscribe.model.mongodb.User;
import com.mindscribe.repository.mongodb.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        System.out.println("Login attempt for username: " + username);

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            System.out.println("Authentication result: " + auth.isAuthenticated());

            // If we get here without exception, authentication succeeded
            User user = userRepository.findByUsername(username).orElse(null);
            System.out.println("Found user: " + (user != null ? user.getUsername() : "null"));
            
            if (user != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Login successful");
                response.put("user", Map.of(
                    "id", user.getId(),
                    "username", username,
                    "email", user.getEmail() != null ? user.getEmail() : ""
                ));
                return ResponseEntity.ok(response);
            } else {
                System.out.println("User not found after authentication for: " + username);
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not found");
                return ResponseEntity.status(401).body(error);
            }
        } catch (Exception e) {
            System.out.println("Authentication exception: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid credentials");
            return ResponseEntity.status(401).body(error);
        }
    }

    public record RegisterRequest(String username, String email, String password) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> registerRequest) {
        String username = registerRequest.get("username");
        String password = registerRequest.get("password");
        
        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Username and password required");
            return ResponseEntity.status(400).body(error);
        }

        if (userRepository.findByUsername(username).isPresent()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Username already exists");
            return ResponseEntity.status(400).body(error);
        }

        String hashed = passwordEncoder.encode(password);
        User user = new User(username, null, hashed, "ROLE_USER");
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User created successfully");
        response.put("user", Map.of(
            "id", user.getId(),
            "username", username,
            "email", ""
        ));
        return ResponseEntity.status(201).body(response);
    }
}
