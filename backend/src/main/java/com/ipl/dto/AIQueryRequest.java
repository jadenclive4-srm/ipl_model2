package com.ipl.dto;

import lombok.Data;
import java.util.Map;

@Data
public class AIQueryRequest {
    private String query;
    private Long matchId;
    private Map<String, String> teams;
    private Map<String, Integer> predictions;
}