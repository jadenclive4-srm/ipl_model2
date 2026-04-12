package com.ipl.repository.mongo;

import com.ipl.model.mongo.QuizCorrectAnswers;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface QuizCorrectAnswersRepository extends MongoRepository<QuizCorrectAnswers, String> {
    
    Optional<QuizCorrectAnswers> findByMatchId(Long matchId);
}