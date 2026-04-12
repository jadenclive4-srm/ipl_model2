package com.ipl.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Map;

@Data
public class QuizPredictionDTO {
    private Long userId;
    private Long matchId;
    
    @JsonProperty("answers")
    private Map<String, String> answers;
}