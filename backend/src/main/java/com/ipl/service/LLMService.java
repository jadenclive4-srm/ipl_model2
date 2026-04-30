package com.ipl.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.*;

@Service
@Slf4j
public class LLMService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 🔥 Multiple SearXNG instances (important)
    private final List<String> searxInstances = List.of(
            "https://search.sapti.me",
            "https://searx.tiekoetter.com",
            "https://search.bus-hit.me"
    );

    // ---------------- GENERATE RESPONSE ----------------
    public String generateResponse(String userQuery, Map<String, Object> predictionData) {
        String prompt = String.format("""
You are an IPL match prediction assistant.

Explain the result clearly based on the match data.

User Question:
%s

Match Data:
Team1: %s
Team2: %s
Winner: %s
Team1 Probability: %s%%
Team2 Probability: %s%%
Reasons: %s
""",
                userQuery,
                predictionData.get("team1"),
                predictionData.get("team2"),
                predictionData.get("winner"),
                predictionData.getOrDefault("team1Probability", 0),
                predictionData.getOrDefault("team2Probability", 0),
                predictionData.getOrDefault("reasons", "N/A")
        );

        return callGroqAPI(prompt);
    }

    // ---------------- GROQ CALL ----------------
    public String callGroqAPI(String prompt) {
        RestTemplate restTemplate = createRestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", groqModel);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1024);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));
        requestBody.put("messages", messages);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "https://api.groq.com/openai/v1/chat/completions",
                request,
                String.class
        );

        return parseGroqResponse(response.getBody());
    }

    // ---------------- SEARCH + GROQ (FIXED) ----------------
    public String searchAndAnswer(String query) {
        RestTemplate restTemplate = createRestTemplate();

        for (String baseUrl : searxInstances) {
            try {
                String searchUrl = baseUrl + "/search?q=" +
                        java.net.URLEncoder.encode(query, "UTF-8") + "&format=json";

                log.info("Trying SearXNG: {}", searchUrl);

                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", "Mozilla/5.0");
                headers.set("Accept", "application/json");

                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<Map> response = restTemplate.exchange(
                        searchUrl,
                        HttpMethod.GET,
                        entity,
                        Map.class
                );

                Map<String, Object> searchData = response.getBody();
                if (searchData == null) continue;

                StringBuilder searchResults = new StringBuilder();
                List<Map<String, Object>> results = (List<Map<String, Object>>) searchData.get("results");

                if (results != null) {
                    int count = 0;
                    for (Map<String, Object> result : results) {
                        if (count >= 5) break;

                        String title = (String) result.get("title");
                        String snippet = (String) result.get("content");

                        if (snippet == null || snippet.length() < 30) continue;

                        searchResults.append(title)
                                .append(": ")
                                .append(snippet)
                                .append("\n\n");

                        count++;
                    }
                }

                if (searchResults.length() == 0) continue;

                String prompt = """
You are an IPL assistant.

STRICT RULES:
- Use ONLY the information provided below.
- DO NOT use your own knowledge.
- If insufficient info, say "I am not sure based on current information."

Search Results:
""" + searchResults + """

Question:
""" + query + """

Answer in 2-4 sentences.
""";

                return callGroqAPI(prompt);

            } catch (Exception e) {
                log.warn("SearXNG failed for {}: {}", baseUrl, e.getMessage());
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {}
        }

        log.warn("All SearXNG instances failed. Using fallback.");
        return fallbackGeneral(query);
    }

    // ---------------- GENERAL ----------------
    public String generalAnswer(String query) {
        String prompt = """
You are an IPL assistant.

Answer clearly in 2-4 sentences.
Do NOT mention knowledge cutoff.

Question:
""" + query;

        return callGroqAPI(prompt);
    }

    // ---------------- TEAM EXTRACTION ----------------
    public Map<String, String> extractTeamsFromQuery(String query) {
        Map<String, String> result = new HashMap<>();
        String q = query.toLowerCase();

        Map<String, String> TEAM_ALIASES = new HashMap<>();
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

        for (Map.Entry<String, String> entry : TEAM_ALIASES.entrySet()) {
            if (q.contains(entry.getKey())) {
                if (!result.containsKey("team1")) {
                    result.put("team1", entry.getValue());
                } else if (!result.containsKey("team2") &&
                        !entry.getValue().equals(result.get("team1"))) {
                    result.put("team2", entry.getValue());
                }
            }
        }

        return result;
    }

    // ---------------- HELPERS ----------------
    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }

    private String fallbackGeneral(String query) {
        return generalAnswer(query);
    }

    private String parseGroqResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            return root.get("choices").get(0).get("message").get("content").asText();
        } catch (Exception e) {
            log.error("Error parsing Groq response: {}", e.getMessage());
            return "Unable to generate response.";
        }
    }
}