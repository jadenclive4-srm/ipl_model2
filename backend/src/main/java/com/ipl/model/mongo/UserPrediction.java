package com.ipl.model.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user_predictions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPrediction {
    
    @Id
    private String id;
    
    private Long userId;
    
    private String username;
    
    private Long matchId;
    
    private Long predictedWinnerId;
    
    private String predictedWinnerName;
    
    private Integer homeProbability;
    
    private Integer awayProbability;
    
    private Boolean isCorrect;
    
    private Integer pointsEarned;
    
    private Long createdAt;
}