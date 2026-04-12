package com.ipl.service;

import com.ipl.model.mongo.UserPoints;
import com.ipl.repository.UserRepository;
import com.ipl.repository.mongo.UserPointsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserPointsService {
    
    private final UserPointsRepository userPointsRepository;
    private final UserRepository userRepository;
    
    public void updateUserPoints(Long userId, Integer pointsToAdd) {
        Optional<UserPoints> existing = userPointsRepository.findByUserId(userId);
        
        if (existing.isPresent()) {
            UserPoints up = existing.get();
            up.setTotalPoints(up.getTotalPoints() + pointsToAdd.longValue());
            up.setLastUpdated(System.currentTimeMillis());
            userPointsRepository.save(up);
        } else {
            userRepository.findById(userId).ifPresent(user -> {
                UserPoints newPoints = new UserPoints();
                newPoints.setUserId(userId);
                newPoints.setUsername(user.getUsername());
                newPoints.setFullName(user.getFullName());
                newPoints.setTotalPoints(pointsToAdd.longValue());
                newPoints.setTotalPredictions(0);
                newPoints.setCorrectPredictions(0);
                newPoints.setLastUpdated(System.currentTimeMillis());
                userPointsRepository.save(newPoints);
            });
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
        List<com.ipl.model.User> users = userRepository.findAll();
        for (com.ipl.model.User user : users) {
            Optional<UserPoints> existing = userPointsRepository.findByUserId(user.getId());
            if (existing.isEmpty()) {
                UserPoints newPoints = new UserPoints();
                newPoints.setUserId(user.getId());
                newPoints.setUsername(user.getUsername());
                newPoints.setFullName(user.getFullName());
                newPoints.setTotalPoints(user.getPoints() != null ? user.getPoints() : 0L);
                newPoints.setTotalPredictions(0);
                newPoints.setCorrectPredictions(0);
                newPoints.setLastUpdated(System.currentTimeMillis());
                userPointsRepository.save(newPoints);
            } else {
                UserPoints up = existing.get();
                up.setTotalPoints(user.getPoints() != null ? user.getPoints() : 0L);
                userPointsRepository.save(up);
            }
        }
    }
}