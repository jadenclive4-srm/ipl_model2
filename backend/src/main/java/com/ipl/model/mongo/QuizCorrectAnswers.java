package com.ipl.model.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import java.util.Map;

@Document(collection = "quiz_correct_answers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizCorrectAnswers {
    
    @Id
    private String id;
    
    private Long matchId;
    
    private Map<String, String> correctAnswers;
    
    private Long updatedAt;
    
    private String updatedBy;
}