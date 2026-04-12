package com.ipl.controller;

import com.ipl.dto.PredictionDTO;
import com.ipl.dto.UserPredictionDTO;
import com.ipl.model.Prediction;
import com.ipl.model.User;
import com.ipl.model.mongo.UserPrediction;
import com.ipl.repository.UserRepository;
import com.ipl.repository.mongo.UserPredictionRepository;
import com.ipl.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class PredictionController {
    
    private final PredictionService predictionService;
    private final UserPredictionRepository userPredictionRepository;
    private final UserRepository userRepository;
    
    @PostMapping
    public ResponseEntity<PredictionDTO> createPrediction(@RequestBody PredictionDTO predictionDTO) {
        Prediction prediction = predictionService.createPrediction(
                predictionDTO.getUserId(),
                predictionDTO.getMatchId(),
                predictionDTO.getPredictedWinnerId(),
                predictionDTO.getHomeProbability(),
                predictionDTO.getAwayProbability()
        );
        return ResponseEntity.ok(convertToDTO(prediction));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PredictionDTO>> getUserPredictions(@PathVariable Long userId) {
        List<PredictionDTO> predictions = predictionService.getUserPredictions(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(predictions);
    }
    
    @GetMapping("/match/{matchId}")
    public ResponseEntity<List<PredictionDTO>> getMatchPredictions(@PathVariable Long matchId) {
        List<PredictionDTO> predictions = predictionService.getMatchPredictions(matchId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(predictions);
    }
    
    @GetMapping("/user/{userId}/match/{matchId}")
    public ResponseEntity<PredictionDTO> getUserMatchPrediction(
            @PathVariable Long userId,
            @PathVariable Long matchId) {
        return predictionService.getUserMatchPrediction(userId, matchId)
                .map(prediction -> ResponseEntity.ok(convertToDTO(prediction)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/evaluate/{matchId}")
    public ResponseEntity<Map<String, String>> evaluatePredictions(@PathVariable Long matchId) {
        try {
            predictionService.evaluatePredictions(matchId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Predictions evaluated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/match/{matchId}/all")
    public ResponseEntity<List<PredictionDTO>> getAllPredictionsForMatch(@PathVariable Long matchId) {
        List<PredictionDTO> predictions = predictionService.getMatchPredictions(matchId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(predictions);
    }
    
    @PostMapping("/reset/{matchId}")
    public ResponseEntity<Map<String, String>> resetPredictions(@PathVariable Long matchId) {
        predictionService.resetPredictions(matchId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Predictions reset successfully");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/quiz")
    public ResponseEntity<Map<String, String>> submitQuizPrediction(@RequestBody com.ipl.dto.QuizPredictionDTO quizDTO) {
        System.out.println("Quiz DTO received: userId=" + quizDTO.getUserId() + ", matchId=" + quizDTO.getMatchId() + ", answers=" + quizDTO.getAnswers());
        predictionService.saveQuizPrediction(
                quizDTO.getUserId(),
                quizDTO.getMatchId(),
                quizDTO.getAnswers()
        );
        Map<String, String> response = new HashMap<>();
        response.put("message", "Quiz submitted successfully!");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/delete/{matchId}")
    public ResponseEntity<Map<String, String>> deleteAllPredictions(@PathVariable Long matchId) {
        predictionService.deleteAllPredictionsForMatchV2(matchId);
        predictionService.deleteAllUserPredictionsFromMongo(matchId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "All predictions deleted successfully");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/all-users-predictions")
    public ResponseEntity<List<UserPredictionDTO>> getAllUsersWithPredictions() {
        List<UserPrediction> mongoPredictions = userPredictionRepository.findAll();
        
        List<UserPredictionDTO> result = mongoPredictions.stream()
            .map(mongoPred -> {
                User user = userRepository.findById(mongoPred.getUserId()).orElse(null);
                String fullName = user != null ? user.getFullName() : mongoPred.getUsername();
                return new UserPredictionDTO(
                    mongoPred.getUserId(),
                    fullName,
                    mongoPred.getPredictedWinnerName(),
                    mongoPred.getMatchId()
                );
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/predictions-by-date")
    public ResponseEntity<List<UserPredictionDTO>> getPredictionsByDate(@RequestParam String date) {
        try {
            java.time.LocalDate localDate = java.time.LocalDate.parse(date);
            long startOfDay = localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endOfDay = localDate.atTime(java.time.LocalTime.MAX).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            List<UserPrediction> mongoPredictions = userPredictionRepository.findByCreatedAtBetween(startOfDay, endOfDay);
            
            List<UserPredictionDTO> result = mongoPredictions.stream()
                .map(mongoPred -> {
                    User user = userRepository.findById(mongoPred.getUserId()).orElse(null);
                    String fullName = user != null ? user.getFullName() : mongoPred.getUsername();
                    return new UserPredictionDTO(
                        mongoPred.getUserId(),
                        fullName,
                        mongoPred.getPredictedWinnerName(),
                        mongoPred.getMatchId()
                    );
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    private PredictionDTO convertToDTO(Prediction prediction) {
        PredictionDTO dto = new PredictionDTO();
        dto.setId(prediction.getId());
        dto.setUserId(prediction.getUser().getId());
        dto.setUsername(prediction.getUser().getUsername());
        dto.setMatchId(prediction.getMatch().getId());
        
        if (prediction.getPredictedWinner() != null) {
            dto.setPredictedWinnerId(prediction.getPredictedWinner().getId());
            dto.setPredictedWinnerName(prediction.getPredictedWinner().getTeamName());
        }
        
        dto.setIsCorrect(prediction.getIsCorrect());
        dto.setPointsEarned(prediction.getPointsEarned());
        dto.setCreatedAt(prediction.getCreatedAt());
        dto.setHomeProbability(prediction.getHomeProbability());
        dto.setAwayProbability(prediction.getAwayProbability());
        
        return dto;
    }
}