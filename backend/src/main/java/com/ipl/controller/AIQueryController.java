package com.ipl.controller;

import com.ipl.dto.AIQueryRequest;
import com.ipl.service.LLMService;
import com.ipl.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIQueryController {
    
    private final MatchService matchService;
    private final LLMService llmService;
    
    @PostMapping("/query")
    public ResponseEntity<Map<String, String>> handleQuery(@RequestBody AIQueryRequest request) {
        String answer = llmService.generateResponse(
            request.getQuery(),
            Map.of("teams", request.getTeams(), "predictions", request.getPredictions())
        );
        return ResponseEntity.ok(Map.of("answer", answer));
    }
    
    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> askQuestion(@RequestBody AskRequest request) {
        String query = request.getQuery() != null ? request.getQuery().toLowerCase() : "";
        Map<String, String> teams = llmService.extractTeamsFromQuery(query);
        
        Map<String, Object> response = new HashMap<>();
        
        if (teams.isEmpty() || teams.get("team1") == null || teams.get("team2") == null) {
            response.put("error", "Please provide valid IPL teams.");
            response.put("teams", teams);
            return ResponseEntity.ok(response);
        }
        
        String team1 = teams.get("team1");
        String team2 = teams.get("team2");
        
        try {
            Map<String, Object> prediction = matchService.predictMatch(team1, team2);
            
            String llmResponse = llmService.generateResponse(request.getQuery(), prediction);
            
            response.put("response", llmResponse);
            response.put("rawData", prediction);
            
        } catch (Exception e) {
            response.put("error", "Could not generate prediction: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/ask")
    public ResponseEntity<Map<String, Object>> askQuestionGet(@RequestParam String query) {
        return askQuestion(new AskRequest(query));
    }
    
    public static class AskRequest {
        private String query;
        
        public AskRequest() {}
        
        public AskRequest(String query) {
            this.query = query;
        }
        
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
    }
}