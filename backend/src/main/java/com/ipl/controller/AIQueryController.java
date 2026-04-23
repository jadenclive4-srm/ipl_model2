package com.ipl.controller;

import com.ipl.dto.AIQueryRequest;
import com.ipl.model.Match;
import com.ipl.service.LLMService;
import com.ipl.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
        String query = request.getQuery() != null ? request.getQuery() : "";
        String intent = detectIntent(query);

        Map<String, Object> response = new HashMap<>();

        try {
            String llmResponse;

            if (intent.equals("MATCH")) {
                // Handle match prediction questions
                Map<String, String> teams = llmService.extractTeamsFromQuery(query.toLowerCase());
                String team1 = null;
                String team2 = null;

                // If no teams found in query, try to get today's match
                if (teams.isEmpty() || teams.get("team1") == null || teams.get("team2") == null) {
                    Optional<Match> todayMatch = matchService.getTodayMatch();
                    if (todayMatch.isPresent()) {
                        Match match = todayMatch.get();
                        team1 = match.getHomeTeam().getTeamName();
                        team2 = match.getAwayTeam().getTeamName();
                    } else {
                        response.put("error", "Please provide valid IPL teams or ensure there's a match scheduled for today.");
                        response.put("teams", teams);
                        return ResponseEntity.ok(response);
                    }
                } else {
                    team1 = teams.get("team1");
                    team2 = teams.get("team2");
                }

                Map<String, Object> prediction = matchService.predictMatch(team1, team2);
                llmResponse = llmService.generateResponse(query, prediction);
                response.put("rawData", prediction);
            } else if (intent.equals("PLAYER")) {
                // Handle player-related questions
                llmResponse = handlePlayerQuery(query);
            } else {
                // Handle general questions
                llmResponse = handleGeneralQuery(query);
            }

            response.put("response", llmResponse);

        } catch (Exception e) {
            response.put("error", "Could not generate response: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/ask")
    public ResponseEntity<Map<String, Object>> askQuestionGet(@RequestParam String query) {
        return askQuestion(new AskRequest(query));
    }
    
    private String detectIntent(String query) {
        if (query == null) return "GENERAL";
        query = query.toLowerCase();

        if (query.contains("vs") || query.contains("versus") || query.contains("match") ||
            query.contains("win") || query.contains("beat") || query.contains("defeat") ||
            query.contains("winner") || query.contains("prediction") || query.contains("predict")) {
            return "MATCH";
        }

        if (query.contains("score") || query.contains("runs") || query.contains("50") ||
            query.contains("100") || query.contains("century") || query.contains("half-century") ||
            query.contains("kohli") || query.contains("rohit") || query.contains("dhoni") ||
            query.contains("captain") || query.contains("batsman") || query.contains("bowler") ||
            query.contains("wicket") || query.contains("player") || query.contains("batting") ||
            query.contains("bowling")) {
            return "PLAYER";
        }

        return "GENERAL";
    }

    private String handlePlayerQuery(String query) {
        String prompt = """
        You are an IPL cricket assistant.

        Answer briefly in 1-2 sentences based on general IPL knowledge.

        Question: %s

        If exact stats are not available, give a reasonable cricket-based answer.
        """.formatted(query);

        return llmService.callGeminiAPI(prompt);
    }

    private String handleGeneralQuery(String query) {
        String prompt = """
        You are an IPL cricket assistant.

        Answer briefly in 1-2 sentences based on general IPL knowledge.

        Question: %s

        Keep answers informative but concise.
        """.formatted(query);

        return llmService.callGeminiAPI(prompt);
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