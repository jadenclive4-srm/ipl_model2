package com.ipl.controller;

import com.ipl.dto.UserDTO;
import com.ipl.model.User;
import com.ipl.model.mongo.UserPoints;
import com.ipl.service.UserPointsService;
import com.ipl.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {
    
    private final UserService userService;
    private final UserPointsService userPointsService;
    
     @GetMapping
     public ResponseEntity<List<UserDTO>> getLeaderboard() {
         List<UserPoints> mongoPoints = userPointsService.getLeaderboard();
         
         List<UserDTO> users = new ArrayList<>();
         Long previousPoints = null;
         int currentRank = 0;
         for (int i = 0; i < mongoPoints.size(); i++) {
             UserPoints up = mongoPoints.get(i);
             if (previousPoints == null || !up.getTotalPoints().equals(previousPoints)) {
                 currentRank = i + 1;
             }
             previousPoints = up.getTotalPoints();
             UserDTO dto = convertMongoPointsToDTO(up);
             dto.setRank(currentRank);
             users.add(dto);
         }
         return ResponseEntity.ok(users);
     }
    
     @GetMapping("/top/{count}")
     public ResponseEntity<List<UserDTO>> getTopUsers(@PathVariable Integer count) {
         List<UserPoints> mongoPoints = userPointsService.getLeaderboard();
         
         List<UserDTO> users = new ArrayList<>();
         Long previousPoints = null;
         int currentRank = 0;
         int processed = 0;
         for (UserPoints up : mongoPoints) {
             if (processed >= count) break;
             if (previousPoints == null || !up.getTotalPoints().equals(previousPoints)) {
                 currentRank = processed + 1;
             }
             previousPoints = up.getTotalPoints();
             UserDTO dto = convertMongoPointsToDTO(up);
             dto.setRank(currentRank);
             users.add(dto);
             processed++;
         }
         return ResponseEntity.ok(users);
     }
    
     @GetMapping("/user/{userId}/rank")
     public ResponseEntity<UserDTO> getUserRank(@PathVariable Long userId) {
         Long points = userPointsService.getUserPoints(userId);
         int rank = userPointsService.getUserRank(userId);
         return userService.findById(userId)
                 .map(user -> {
                     UserDTO dto = new UserDTO();
                     dto.setId(user.getId());
                     dto.setUsername(user.getUsername());
                     dto.setEmail(user.getEmail());
                     dto.setFullName(user.getFullName());
                     dto.setPoints(points);
                     dto.setRank(rank);
                     dto.setRole(user.getRole());
                     dto.setCreatedAt(user.getCreatedAt());
                     return ResponseEntity.ok(dto);
                 })
                 .orElse(ResponseEntity.notFound().build());
     }
    
    @GetMapping("/user/{userId}/points")
    public ResponseEntity<Long> getUserPoints(@PathVariable Long userId) {
        Long points = userPointsService.getUserPoints(userId);
        return ResponseEntity.ok(points);
    }
    
    @PostMapping("/sync")
    public ResponseEntity<String> syncUserPoints() {
        userPointsService.syncAllUserPoints();
        return ResponseEntity.ok("User points synced");
    }
    
    @PostMapping("/cleanup")
    public ResponseEntity<String> cleanupTestUsers() {
        userPointsService.cleanupAndSync();
        return ResponseEntity.ok("Test users cleaned up and synced");
     }
     
     private UserDTO convertMongoPointsToDTO(UserPoints up) {
         UserDTO dto = new UserDTO();
         dto.setId(up.getUserId());
         dto.setUsername(up.getUsername());
         dto.setFullName(up.getFullName());
         dto.setPoints(up.getTotalPoints());
         return dto;
     }
  }