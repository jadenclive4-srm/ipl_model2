package com.ipl.repository;

import com.ipl.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByUniqueUserId(String uniqueUserId);
    
    Optional<User> findByUniqueUserId(String uniqueUserId);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u ORDER BY u.points DESC")
    List<User> findAllByOrderByPointsDesc();
    
    List<User> findAllByOrderByRankAsc();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.points > :points")
    Long countUsersWithMorePoints(Integer points);
    
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.points = u.points + :points WHERE u.id = :userId")
    void incrementPoints(Long userId, int points);
}