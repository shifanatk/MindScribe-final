package com.mindscribe.controller;

import com.mindscribe.model.mongodb.User;
import com.mindscribe.repository.mongodb.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/mongodb")
    public ResponseEntity<?> testMongoDB() {
        Map<String, Object> response = new HashMap<>();
        try {
            // Test MongoDB connection by counting users
            long userCount = userRepository.count();
            response.put("status", "success");
            response.put("message", "MongoDB connection working");
            response.put("userCount", userCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "MongoDB connection failed");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/create-user")
    public ResponseEntity<?> createTestUser() {
        Map<String, Object> response = new HashMap<>();
        try {
            User testUser = new User("testuser", "test@example.com", "hashedpassword", "ROLE_USER");
            User saved = userRepository.save(testUser);
            response.put("status", "success");
            response.put("message", "Test user created");
            response.put("userId", saved.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to create user");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
