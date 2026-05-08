package com.ipl.controller;

import com.ipl.model.mongo.UserMongo;
import com.ipl.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    @GetMapping("/export-users-csv")
    public ResponseEntity<String> exportUsersToCsv() {
        try {
            List<UserMongo> users = userService.getAllUsersFromMongo();

            StringBuilder csv = new StringBuilder();
            csv.append("username,email,fullName,role\n");

            for (UserMongo user : users) {
                csv.append(user.getUsername()).append(",")
                   .append(user.getEmail()).append(",")
                   .append(user.getFullName() != null ? user.getFullName() : "").append(",")
                   .append(user.getRole() != null ? user.getRole() : "USER").append("\n");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", "users.csv");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csv.toString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error exporting users: " + e.getMessage());
        }
    }

    @PostMapping("/import-users-csv")
    public ResponseEntity<Map<String, Object>> importUsersFromCsv(@RequestParam("file") MultipartFile file) {
        try {
            List<String> errors = new ArrayList<>();
            int successCount = 0;
            int totalCount = 0;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                String line;
                boolean isFirstLine = true;

                while ((line = reader.readLine()) != null) {
                    totalCount++;
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue; // Skip header
                    }

                    String[] parts = line.split(",");
                    if (parts.length < 2) {
                        errors.add("Line " + totalCount + ": Invalid format, expected at least username,email");
                        continue;
                    }

                    String username = parts[0].trim();
                    String email = parts[1].trim();
                    String fullName = parts.length > 2 ? parts[2].trim() : "";
                    String role = parts.length > 3 ? parts[3].trim() : "USER";
                    String password = "user123"; // Default password as requested

                    if (username.isEmpty() || email.isEmpty()) {
                        errors.add("Line " + totalCount + ": Username and email are required");
                        continue;
                    }

                    try {
                        UserMongo user = userService.createUserMongoOnly(
                            username,
                            null, // uniqueUserId will be auto-generated
                            email,
                            password,
                            fullName,
                            role
                        );
                        successCount++;
                    } catch (Exception e) {
                        errors.add("Line " + totalCount + ": Failed to create user '" + username + "': " + e.getMessage());
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Bulk user import completed");
            response.put("totalProcessed", totalCount - 1); // Exclude header
            response.put("successCount", successCount);
            response.put("errorCount", errors.size());
            response.put("errors", errors);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to import users: " + e.getMessage());
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