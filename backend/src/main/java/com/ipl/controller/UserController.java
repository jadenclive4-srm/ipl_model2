package com.ipl.controller;

import com.ipl.dto.UserDTO;
import com.ipl.model.User;
import com.ipl.model.mongo.UserMongo;
import com.ipl.service.UserService;
import com.ipl.service.UserPointsService;
import com.ipl.repository.mongo.UserMongoRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;
    private final UserPointsService userPointsService;
    private final UserMongoRepository userMongoRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, UserPointsService userPointsService, 
                          UserMongoRepository userMongoRepository, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.userPointsService = userPointsService;
        this.userMongoRepository = userMongoRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserDTO> userDTOs = users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(convertToDTO(user)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/username/{username}")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(user -> ResponseEntity.ok(convertToDTO(user)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @RequestBody UserDTO userDTO) {
        User updatedUser = userService.updateUser(
                id,
                userDTO.getFullName(),
                userDTO.getEmail()
        );
        return ResponseEntity.ok(convertToDTO(updatedUser));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/mongo")
    public ResponseEntity<?> createMongoUser(@RequestBody UserMongo userMongo) {
        try {
            if (userMongoRepository.existsByUsername(userMongo.getUsername())) {
                return ResponseEntity.badRequest().body("Username already exists in MongoDB");
            }
            if (userMongoRepository.existsByEmail(userMongo.getEmail())) {
                return ResponseEntity.badRequest().body("Email already exists in MongoDB");
            }
            
            userMongo.setId(System.currentTimeMillis());
            userMongo.setUniqueUserId(userMongo.getUsername() + "-" + System.currentTimeMillis());
            userMongo.setPassword(passwordEncoder.encode(userMongo.getPassword()));
            userMongo.setIsActive(true);
            userMongo.setEmailVerified(true);
            userMongo.setCreatedAt(System.currentTimeMillis());
            userMongo.setUpdatedAt(System.currentTimeMillis());
            if (userMongo.getRole() == null) {
                userMongo.setRole("USER");
            }
            if (userMongo.getPoints() == null) {
                userMongo.setPoints(0);
            }
            if (userMongo.getRank() == null) {
                userMongo.setRank(0);
            }
            
            UserMongo saved = userMongoRepository.save(userMongo);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to create user: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> updatePassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        userService.updatePassword(id, request.get("password"));
        return ResponseEntity.ok().build();
    }
    
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId() != null ? user.getId().longValue() : null);
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        // Get points and rank from UserPointsService (MongoDB)
        Long points = userPointsService.getUserPoints(user.getId());
        dto.setPoints(points);
        int rank = userPointsService.getUserRank(user.getId());
        dto.setRank(rank);
        dto.setRole(user.getRole());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}