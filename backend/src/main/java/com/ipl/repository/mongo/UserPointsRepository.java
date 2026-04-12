package com.ipl.repository.mongo;

import com.ipl.model.mongo.UserPoints;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserPointsRepository extends MongoRepository<UserPoints, String> {
    Optional<UserPoints> findByUserId(Long userId);
    List<UserPoints> findAllByOrderByTotalPointsDesc();
}