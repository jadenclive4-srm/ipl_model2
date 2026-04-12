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
    
    @Value("${anthropic.api.key:}")
    private String apiKey;
    
    @Value("${anthropic.model:claude-3-5-sonnet-20241022}")
    private String model;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public String generateResponse(String userQuery, Map<String, Object> predictionData) {
        if (apiKey == null || apiKey.isBlank()) {
            return generateFallbackResponse(userQuery, predictionData);
        }
        
        try {
            String prompt = "You are an IPL prediction agent. " +
                "User question: " + userQuery + ". Prediction data: " + predictionData.toString() + ". " +
                "Generate a short, clear explanation including: Winner, Probability, Key reasons.";
            
            String response = callAnthropicAPI(prompt);
            return response != null ? response : generateFallbackResponse(userQuery, predictionData);
            
        } catch (RestClientException e) {
            log.error("Error calling Anthropic API: {}", e.getMessage());
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
    
    private String callAnthropicAPI(String prompt) {
        RestTemplate restTemplate = new RestTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", 300);
        
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);
        requestBody.put("messages", messages);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            "https://api.anthropic.com/v1/messages",
            request,
            String.class
        );
        
        return parseResponse(response.getBody());
    }
    
    private String parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.get("content");
            if (content != null && content.isArray() && content.size() > 0) {
                return content.get(0).get("text").asText();
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
        
        if (userQuery == null) userQuery = "";
        userQuery = userQuery.toLowerCase();
        
        if (userQuery.contains("who") || userQuery.contains("win") || userQuery.contains("winner") || userQuery.contains("predict")) {
            int winnerProb = winner.equals(team1) ? team1Prob : team2Prob;
            return winner + " are likely to win with a " + winnerProb + "% probability based on strong past performance and better standings.";
        }
        
        if (userQuery.contains("why") || userQuery.contains("reason") || userQuery.contains("explain")) {
            StringBuilder sb = new StringBuilder();
            sb.append(winner).append(" is favored because: ");
            
            if (predictionData.containsKey("reasons")) {
                List<String> reasons = (List<String>) predictionData.get("reasons");
                for (int i = 0; i < Math.min(2, reasons.size()); i++) {
                    sb.append(reasons.get(i));
                    if (i == 0) sb.append(". ");
                }
            }
            
            return sb.toString();
        }
        
        if (userQuery.contains("probability") || userQuery.contains("chance") || userQuery.contains("odds")) {
            String better = team1Prob > team2Prob ? team1 : team2;
            return "Current prediction probabilities:\n- " + team1 + ": " + team1Prob + "%\n- " + team2 + ": " + team2Prob + "%\n\n" + better + " has a better chance of winning.";
        }
        
        if (userQuery.contains("underrated") || userQuery.contains("underdog") || userQuery.contains("upset")) {
            String underdog = team1Prob < team2Prob ? team1 : team2;
            int underdogProb = Math.min(team1Prob, team2Prob);
            return underdog + " might be undervalued at " + underdogProb + "%. They could be a dark horse if conditions favor them!";
        }
        
        if (userQuery.contains("h2h") || userQuery.contains("head") || userQuery.contains("record") || userQuery.contains("history")) {
            if (predictionData.containsKey("headToHead")) {
                Map<String, Object> h2h = (Map<String, Object>) predictionData.get("headToHead");
                int total = (int) h2h.getOrDefault("totalMatches", 0);
                int t1Wins = (int) h2h.getOrDefault("team1Wins", 0);
                int t2Wins = (int) h2h.getOrDefault("team2Wins", 0);
                
                if (total > 0) {
                    return "Head-to-head record: " + total + " matches total. " + team1 + " has won " + t1Wins + " times, " + team2 + " has won " + t2Wins + " times.";
                }
            }
            return "No historical head-to-head data available for these teams.";
        }
        
        int winnerProb = winner.equals(team1) ? team1Prob : team2Prob;
        return "Analysis for " + team1 + " vs " + team2 + ": " + winner + " is predicted to win with " + winnerProb + "% probability. Ask me about reasons, probabilities, or head-to-head record!";
    }
}