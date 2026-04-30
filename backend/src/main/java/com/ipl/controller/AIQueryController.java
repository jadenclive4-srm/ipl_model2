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
                Map<String, String> teams = llmService.extractTeamsFromQuery(query.toLowerCase());
                String team1;
                String team2;

                // If teams not found → fallback to today's match
                if (teams.isEmpty() || teams.get("team1") == null || teams.get("team2") == null) {
                    Optional<Match> todayMatch = matchService.getTodayMatch();
                    if (todayMatch.isPresent()) {
                        Match match = todayMatch.get();
                        team1 = match.getHomeTeam().getTeamName();
                        team2 = match.getAwayTeam().getTeamName();
                    } else {
                        response.put("error", "Please provide valid IPL teams or ensure a match is scheduled.");
                        return ResponseEntity.ok(response);
                    }
                } else {
                    team1 = teams.get("team1");
                    team2 = teams.get("team2");
                }

                Map<String, Object> prediction = matchService.predictMatch(team1, team2);
                llmResponse = llmService.generateResponse(query, prediction);
                response.put("rawData", prediction);

            } else if (intent.equals("LATEST")) {
                // 🔥 Uses SearXNG + Groq
                llmResponse = llmService.searchAndAnswer(query);

            } else if (intent.equals("PLAYER")) {
                llmResponse = handlePlayerQuery(query);

            } else {
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
    
    // ---------------- INTENT DETECTION ----------------
    private String detectIntent(String query) {
        if (query == null) return "GENERAL";
        query = query.toLowerCase();

        // 🔥 MATCH (highest priority)
        if (query.contains("vs") || query.contains("versus") || query.contains("match") ||
            query.contains("who will win") || query.contains("prediction") || query.contains("predict")) {
            return "MATCH";
        }

        // 🔥 LATEST (real-time only)
        if (query.contains("latest") || query.contains("news") ||
            query.contains("update") || query.contains("recent") ||
            query.contains("injury") || query.contains("transfer") ||
            query.contains("announcement")) {
            return "LATEST";
        }

        // 🔥 PLAYER
        if (query.contains("score") || query.contains("runs") ||
            query.contains("kohli") || query.contains("rohit") ||
            query.contains("dhoni") || query.contains("captain") ||
            query.contains("player") || query.contains("batting") ||
            query.contains("bowling") || query.contains("wicket")) {
            return "PLAYER";
        }

        return "GENERAL";
    }

    // ---------------- PLAYER ----------------
    private String handlePlayerQuery(String query) {
        String prompt = """
You are an IPL cricket assistant.

Answer briefly in 1-2 sentences based on cricket knowledge.
Do NOT mention knowledge cutoff.

Question:
""" + query;

        return llmService.callGroqAPI(prompt);
    }

    // ---------------- GENERAL ----------------
    private String handleGeneralQuery(String query) {
        String prompt = """
You are an IPL cricket assistant.

Answer briefly in 1-2 sentences.
Do NOT mention knowledge cutoff.

Question:
""" + query;

        return llmService.callGroqAPI(prompt);
    }

    // ---------------- REQUEST DTO ----------------
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