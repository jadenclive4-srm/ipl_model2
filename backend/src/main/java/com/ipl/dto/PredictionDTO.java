package com.ipl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionDTO {
    
    private Long id;
    private Long userId;
    private String username;
    private Long matchId;
    private Long predictedWinnerId;
    private String predictedWinnerName;
    private Boolean isCorrect;
    private Integer pointsEarned;
    private Long createdAt;
    private Integer homeProbability;
    private Integer awayProbability;
}