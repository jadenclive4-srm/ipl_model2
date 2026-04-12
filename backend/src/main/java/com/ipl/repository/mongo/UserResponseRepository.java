package com.ipl.repository.mongo;

import com.ipl.model.mongo.UserResponse;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserResponseRepository extends MongoRepository<UserResponse, String> {
    
    Optional<UserResponse> findByUserIdAndMatchId(Long userId, Long matchId);
    
    List<UserResponse> findByUserId(Long userId);
    
    List<UserResponse> findByMatchId(Long matchId);
    
    boolean existsByUserIdAndMatchId(Long userId, Long matchId);
}