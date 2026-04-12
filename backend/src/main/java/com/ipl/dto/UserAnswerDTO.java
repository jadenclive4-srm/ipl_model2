package com.ipl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAnswerDTO {
    
    private Long id;
    private Long userId;
    private String username;
    private Long questionId;
    private String selectedOption;
    private Boolean isCorrect;
    private Integer pointsEarned;
    private Long answeredAt;
}