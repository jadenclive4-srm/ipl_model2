package com.ipl.repository;

import com.ipl.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    @Query("SELECT q FROM Question q WHERE q.match.id = :matchId AND q.isActive = true")
    List<Question> findActiveQuestionsByMatchId(Long matchId);
    
    @Query("SELECT q FROM Question q WHERE q.match.id = :matchId")
    List<Question> findAllByMatchId(Long matchId);
    
    @Query("SELECT q FROM Question q WHERE q.questionType = :questionType AND q.isActive = true")
    List<Question> findActiveQuestionsByType(String questionType);
}