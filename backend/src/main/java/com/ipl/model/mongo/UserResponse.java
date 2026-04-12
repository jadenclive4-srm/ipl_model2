package com.ipl.model.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "user_responses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    @Id
    private String id;
    
    private Long userId;
    
    private String username;
    
    private Long matchId;
    
    private List<QuestionResponse> responses;
    
    private Long createdAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionResponse {
        private String questionId;
        private String selectedOption;
        private Boolean isCorrect;
        private Integer pointsEarned;
    }
}