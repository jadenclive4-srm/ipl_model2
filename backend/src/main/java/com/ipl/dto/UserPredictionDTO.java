package com.ipl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPredictionDTO {
    private Long userId;
    private String fullName;
    private String predictedTeamName;
    private Long matchId;
}