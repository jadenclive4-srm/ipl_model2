package com.ipl.repository;

import com.ipl.model.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {
    
    @Query("SELECT ua FROM UserAnswer ua WHERE ua.user.id = :userId AND ua.question.id = :questionId")
    Optional<UserAnswer> findByUserIdAndQuestionId(Long userId, Long questionId);
    
    @Query("SELECT ua FROM UserAnswer ua WHERE ua.user.id = :userId AND ua.question.match.id = :matchId")
    List<UserAnswer> findByUserIdAndMatchId(Long userId, Long matchId);
    
    @Query("SELECT ua FROM UserAnswer ua WHERE ua.question.id = :questionId")
    List<UserAnswer> findByQuestionId(Long questionId);
    
    boolean existsByUserIdAndQuestionId(Long userId, Long questionId);
}