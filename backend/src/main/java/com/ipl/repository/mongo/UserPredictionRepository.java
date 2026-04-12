package com.ipl.repository.mongo;

import com.ipl.model.mongo.UserPrediction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserPredictionRepository extends MongoRepository<UserPrediction, String> {
    
    Optional<UserPrediction> findByUserIdAndMatchId(Long userId, Long matchId);
    
    List<UserPrediction> findByUserId(Long userId);
    
    List<UserPrediction> findByMatchId(Long matchId);
    
    boolean existsByUserIdAndMatchId(Long userId, Long matchId);
    
    List<UserPrediction> findByCreatedAtBetween(Long startTime, Long endTime);
}