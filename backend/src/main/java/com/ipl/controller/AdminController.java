package com.ipl.controller;

import com.ipl.model.mongo.UserMongo;
import com.ipl.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody AdminCreateUserRequest request) {
        try {
            // Create user in MongoDB only
            UserMongo user = userService.createUserMongoOnly(
                request.getUsername(),
                request.getUniqueUserId(),
                request.getEmail(),
                request.getPassword(),
                request.getFullName(),
                request.getRole() != null ? request.getRole() : "USER"
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User created successfully");
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to create user: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // Request DTO for creating users
    public static class AdminCreateUserRequest {
        private String username;
        private String uniqueUserId;
        private String email;
        private String password;
        private String fullName;
        private String role;

        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getUniqueUserId() { return uniqueUserId; }
        public void setUniqueUserId(String uniqueUserId) { this.uniqueUserId = uniqueUserId; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}