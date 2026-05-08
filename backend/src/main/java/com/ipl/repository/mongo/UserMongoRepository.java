package com.ipl.repository.mongo;

import com.ipl.model.mongo.UserMongo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserMongoRepository extends MongoRepository<UserMongo, Long> {
    Optional<UserMongo> findByUsername(String username);
    Optional<UserMongo> findByEmail(String email);
    Optional<UserMongo> findByUniqueUserId(String uniqueUserId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
