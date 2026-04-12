package com.ipl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDTO {
    
    private Long id;
    private Long matchId;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctOption;
    private Integer pointsValue;
    private Boolean isActive;
    private String questionType;
    private Long createdAt;
    private String userAnswer;
    private Boolean hasAnswered;
}