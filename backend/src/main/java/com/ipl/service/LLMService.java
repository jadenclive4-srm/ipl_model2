package com.ipl.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;

import java.util.*;

@Service
@Slf4j
public class LLMService {
    
    private static final Map<String, String> TEAM_ALIASES = new HashMap<>();
    static {
        TEAM_ALIASES.put("kkr", "Kolkata Knight Riders");
        TEAM_ALIASES.put("kolkata", "Kolkata Knight Riders");
        TEAM_ALIASES.put("pbks", "Punjab Kings");
        TEAM_ALIASES.put("punjab", "Punjab Kings");
        TEAM_ALIASES.put("mi", "Mumbai Indians");
        TEAM_ALIASES.put("mumbai", "Mumbai Indians");
        TEAM_ALIASES.put("csk", "Chennai Super Kings");
        TEAM_ALIASES.put("chennai", "Chennai Super Kings");
        TEAM_ALIASES.put("rcb", "Royal Challengers Bangalore");
        TEAM_ALIASES.put("bangalore", "Royal Challengers Bangalore");
        TEAM_ALIASES.put("srh", "Sunrisers Hyderabad");
        TEAM_ALIASES.put("hyderabad", "Sunrisers Hyderabad");
        TEAM_ALIASES.put("rr", "Rajasthan Royals");
        TEAM_ALIASES.put("rajasthan", "Rajasthan Royals");
        TEAM_ALIASES.put("gt", "Gujarat Titans");
        TEAM_ALIASES.put("gujarat", "Gujarat Titans");
        TEAM_ALIASES.put("lsg", "Lucknow Super Giants");
        TEAM_ALIASES.put("lucknow", "Lucknow Super Giants");
        TEAM_ALIASES.put("dc", "Delhi Capitals");
        TEAM_ALIASES.put("delhi", "Delhi Capitals");
    }
    
    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${google.model:gemini-2.5-flash}")
    private String model;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public String generateResponse(String userQuery, Map<String, Object> predictionData) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key is not configured. Using fallback response.");
            return generateFallbackResponse(userQuery, predictionData);
        }
        
        try {
            String data = String.format("""
Team1: %s
Team2: %s
Winner: %s
Team1 Probability: %d%%
Team2 Probability: %d%%
Reasons: %s
""",
                    predictionData.get("team1"),
                    predictionData.get("team2"),
                    predictionData.get("winner"),
                    predictionData.get("team1Probability"),
                    predictionData.get("team2Probability"),
                    predictionData.get("reasons")
            );

            String prompt = """
You are an IPL match prediction assistant.

Answer in 1 short sentence based ONLY on the match data provided.

User Question:
%s

Match Data:
%s

Keep your answer concise and directly relevant to the question.
""".formatted(userQuery, data);

            String response = callGeminiAPI(prompt);
            return response != null ? response : generateFallbackResponse(userQuery, predictionData);

        } catch (RestClientException e) {
            log.error("Error calling Gemini API: {}", e.getMessage());
            return generateFallbackResponse(userQuery, predictionData);
        } catch (Exception e) {
            log.error("Error generating response: {}", e.getMessage());
            return generateFallbackResponse(userQuery, predictionData);
        }
    }
    
    public Map<String, String> extractTeamsFromQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isBlank()) {
            return result;
        }
        
        String lowerQuery = query.toLowerCase();
        
        for (Map.Entry<String, String> alias : TEAM_ALIASES.entrySet()) {
            if (lowerQuery.contains(alias.getKey())) {
                if (result.get("team1") == null) {
                    result.put("team1", alias.getValue());
                } else if (result.get("team2") == null && !alias.getValue().equals(result.get("team1"))) {
                    result.put("team2", alias.getValue());
                }
            }
        }
        
        return result;
    }
    
    public String callGeminiAPI(String prompt) {
        log.info("MODEL USED: {}", model);
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();

        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, String>> parts = new ArrayList<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", prompt);
        parts.add(part);
        content.put("parts", parts);
        contents.add(content);
        requestBody.put("contents", contents);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey,
            request,
            String.class
        );

        return parseResponse(response.getBody());
    }
    
    private String parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode candidates = root.get("candidates");
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).get("content");
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        return parts.get(0).get("text").asText();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Error parsing response: {}", e.getMessage());
            return null;
        }
    }
    
    private String generateFallbackResponse(String userQuery, Map<String, Object> predictionData) {
        String team1 = (String) predictionData.get("team1");
        String team2 = (String) predictionData.get("team2");
        String winner = (String) predictionData.get("winner");

        int team1Prob = predictionData.get("team1Probability") != null ? (int) predictionData.get("team1Probability") : 50;
        int team2Prob = predictionData.get("team2Probability") != null ? (int) predictionData.get("team2Probability") : 50;

        return String.format(
            "%s is likely to win against %s (%d%% vs %d%%).",
            winner,
            winner.equals(team1) ? team2 : team1,
            Math.max(team1Prob, team2Prob),
            Math.min(team1Prob, team2Prob)
        );
    }
}