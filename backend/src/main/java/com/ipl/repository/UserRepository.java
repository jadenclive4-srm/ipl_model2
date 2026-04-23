package com.ipl.repository;

import com.ipl.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
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
    
    List<User> findAllByOrderByRankAsc();
}