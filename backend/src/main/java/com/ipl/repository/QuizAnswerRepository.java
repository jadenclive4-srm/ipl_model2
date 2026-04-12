package com.ipl.repository;

import com.ipl.model.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
    
    List<QuizAnswer> findByUserIdAndMatchId(Long userId, Long matchId);
    
    Optional<QuizAnswer> findByUserIdAndMatchIdAndQuestionId(Long userId, Long matchId, String questionId);
    
    boolean existsByUserIdAndMatchId(Long userId, Long matchId);
    
    boolean existsByUserIdAndMatchIdAndQuestionId(Long userId, Long matchId, String questionId);
}
