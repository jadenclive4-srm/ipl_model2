package com.ipl.dto;

import lombok.Data;

@Data
public class MatchLeaderboardEntryDTO {
    private Long userId;
    private String username;
    private String fullName;
    private Long predictionPoints;
    private Long quizPoints;
    private Long totalPoints;
    private Integer rank;
}