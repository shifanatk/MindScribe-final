package com.mindscribe.controller;

import com.mindscribe.repository.mongodb.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/debug")
@CrossOrigin(origins = "*")
public class DebugController {

    private final UserRepository userRepository;

    public DebugController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/mongodb-users")
    public String getMongoDBUsers() {
        try {
            List<String> users = userRepository.findAll().stream()
                    .map(user -> user.getUsername())
                    .toList();
            return "{\"users\": " + users + ", \"count\": " + users.size() + "}";
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @GetMapping("/mongodb-connection")
    public String testMongoConnection() {
        try {
            long count = userRepository.count();
            return "{\"connected\": true, \"user_count\": " + count + "}";
        } catch (Exception e) {
            return "{\"connected\": false, \"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
