package com.ipl.service;

import com.ipl.model.User;
import com.ipl.model.mongo.UserPoints;
import com.ipl.model.mongo.UserMongo;
import com.ipl.repository.UserRepository;
import com.ipl.repository.mongo.UserMongoRepository;
import com.ipl.repository.mongo.UserPointsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserPointsService {
    
    private final UserPointsRepository userPointsRepository;
    private final UserRepository userRepository;
    private final UserMongoRepository userMongoRepository;
    
    @Transactional
    public void updateUserPoints(Long userId, Integer pointsToAdd) {
        System.out.println("updateUserPoints called: userId=" + userId + ", points=" + pointsToAdd);
        
        Optional<UserPoints> existing = userPointsRepository.findByUserId(userId);
        
        if (existing.isPresent()) {
            UserPoints up = existing.get();
            up.setTotalPoints(up.getTotalPoints() + pointsToAdd.longValue());
            up.setLastUpdated(System.currentTimeMillis());
            userPointsRepository.save(up);
            System.out.println("Updated existing userPoints for userId=" + userId + ", new total=" + up.getTotalPoints());
        } else {
            // Try H2 first, then MongoDB
            Optional<User> h2User = userRepository.findById(userId);
            String username = null;
            String fullName = null;
            
            if (h2User.isPresent()) {
                username = h2User.get().getUsername();
                fullName = h2User.get().getFullName();
                System.out.println("Found user in H2: " + username);
            } else {
                // Try MongoDB
                try {
                    Optional<com.ipl.model.mongo.UserMongo> mongoUser = userMongoRepository.findById(userId);
                    if (mongoUser.isPresent()) {
                        username = mongoUser.get().getUsername();
                        fullName = mongoUser.get().getFullName();
                        System.out.println("Found user in MongoDB: " + username);
                    }
                } catch (Exception e) {
                    System.err.println("MongoDB user lookup failed: " + e.getMessage());
                }
            }
            
            if (username != null) {
                UserPoints newPoints = new UserPoints();
                newPoints.setUserId(userId);
                newPoints.setUsername(username);
                newPoints.setFullName(fullName);
                newPoints.setTotalPoints(pointsToAdd.longValue());
                newPoints.setTotalPredictions(0);
                newPoints.setCorrectPredictions(0);
                newPoints.setLastUpdated(System.currentTimeMillis());
                userPointsRepository.save(newPoints);
                System.out.println("Created new userPoints for userId=" + userId + ", username=" + username);
            } else {
                System.err.println("User not found in H2 or MongoDB for userId=" + userId);
            }
        }
    }
    
    public Long getUserPoints(Long userId) {
        return userPointsRepository.findByUserId(userId)
            .map(UserPoints::getTotalPoints)
            .orElse(0L);
    }
    
    public List<UserPoints> getLeaderboard() {
        return userPointsRepository.findAllByOrderByTotalPointsDesc();
    }
    
    public void syncAllUserPoints() {
        // Sync from H2 users
        List<com.ipl.model.User> users = userRepository.findAll();
        for (com.ipl.model.User user : users) {
            syncUser(user.getId(), user.getUsername(), user.getFullName(), user.getPoints());
        }
        
        // Sync from MongoDB users
        try {
            List<com.ipl.model.mongo.UserMongo> mongoUsers = userMongoRepository.findAll();
            for (com.ipl.model.mongo.UserMongo mongoUser : mongoUsers) {
                syncUser(mongoUser.getId(), mongoUser.getUsername(), mongoUser.getFullName(), mongoUser.getPoints());
            }
        } catch (Exception e) {
            System.err.println("MongoDB sync error: " + e.getMessage());
        }
    }
    
    private void syncUser(Long userId, String username, String fullName, Integer points) {
        Optional<UserPoints> existing = userPointsRepository.findByUserId(userId);
        if (existing.isEmpty()) {
            UserPoints newPoints = new UserPoints();
            newPoints.setUserId(userId);
            newPoints.setUsername(username);
            newPoints.setFullName(fullName);
            newPoints.setTotalPoints(points != null ? Long.valueOf(points) : 0L);
            newPoints.setTotalPredictions(0);
            newPoints.setCorrectPredictions(0);
            newPoints.setLastUpdated(System.currentTimeMillis());
            userPointsRepository.save(newPoints);
            System.out.println("Synced user: " + username);
        } else {
            UserPoints up = existing.get();
            // Update username/name in case changed
            up.setUsername(username);
            up.setFullName(fullName);
            up.setTotalPoints(points != null ? Long.valueOf(points) : 0L);
            userPointsRepository.save(up);
        }
    }
    
    public void cleanupAndSync() {
        // Delete all test users (user, user1, user2, etc.)
        List<String> testUsernames = List.of("user", "user1", "user2", "user3", "user4", "user5");
        List<UserPoints> allPoints = userPointsRepository.findAll();
        
        for (UserPoints up : allPoints) {
            if (testUsernames.contains(up.getUsername())) {
                userPointsRepository.delete(up);
                System.out.println("Deleted test user: " + up.getUsername());
            }
        }
        
        // Resync all real users
        syncAllUserPoints();
    }
}