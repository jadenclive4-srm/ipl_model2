package com.ipl.controller;

import com.ipl.dto.PredictionDTO;
import com.ipl.dto.UserPredictionDTO;
import com.ipl.model.Match;
import com.ipl.model.Prediction;
import com.ipl.model.User;
import com.ipl.model.mongo.UserPrediction;
import com.ipl.repository.MatchRepository;
import com.ipl.repository.UserRepository;
import com.ipl.repository.mongo.UserPredictionRepository;
import com.ipl.repository.mongo.UserResponseRepository;
import com.ipl.service.PredictionService;
import com.ipl.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;
    private final UserService userService;
    private final UserPredictionRepository userPredictionRepository;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final UserResponseRepository userResponseRepository;
    
    @PostMapping
    public ResponseEntity<?> createPrediction(@RequestBody PredictionDTO predictionDTO) {
        try {
            // Get the authenticated user instead of trusting userId from request
            User authenticatedUser = userService.getCurrentAuthenticatedUser();

            Prediction prediction = predictionService.createPrediction(
                    authenticatedUser.getId(),
                    predictionDTO.getMatchId(),
                    predictionDTO.getPredictedWinnerId(),
                    predictionDTO.getHomeProbability(),
                    predictionDTO.getAwayProbability()
            );
            return ResponseEntity.ok(convertToDTO(prediction));
        } catch (RuntimeException e) {
            System.out.println("Error creating prediction: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PredictionDTO>> getUserPredictions(@PathVariable Long userId) {
        // Get the authenticated user instead of trusting userId from path
        User authenticatedUser = userService.getCurrentAuthenticatedUser();

        // Temporarily only use MongoDB for all users
        List<UserPrediction> mongoPreds = userPredictionRepository.findByUserId(authenticatedUser.getId());
        List<PredictionDTO> predictions = mongoPreds.stream()
                .map(this::convertFromMongoToDTO)
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
    public ResponseEntity<PredictionDTO> getUserMatchPrediction(@PathVariable Long userId, @PathVariable Long matchId) {
        // Get the authenticated user
        User authenticatedUser = userService.getCurrentAuthenticatedUser();

        // Temporarily only use MongoDB for all users
        Optional<UserPrediction> mongoPred = userPredictionRepository.findByUserIdAndMatchId(authenticatedUser.getId(), matchId);
        if (mongoPred.isPresent()) {
            return ResponseEntity.ok(convertFromMongoToDTO(mongoPred.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/evaluate/{matchId}")
    public ResponseEntity<Map<String, String>> evaluatePredictions(@PathVariable Long matchId) {
        try {
            // Validate match exists and has winner before starting transaction
            Match match = matchRepository.findById(matchId).orElse(null);
            if (match == null) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Match not found");
                return ResponseEntity.badRequest().body(response);
            }

            if (match.getWinnerTeam() == null) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Match winner not determined");
                return ResponseEntity.badRequest().body(response);
            }

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

        // Also include MongoDB predictions for admin-created users
        List<UserPrediction> mongoPredictions = userPredictionRepository.findByMatchId(matchId);
        for (UserPrediction mongoPred : mongoPredictions) {
            // Check if this MongoDB prediction already has a corresponding H2 prediction
            boolean alreadyIncluded = predictions.stream()
                    .anyMatch(dto -> dto.getUserId().equals(mongoPred.getUserId()));

            if (!alreadyIncluded) {
                predictions.add(convertFromMongoToDTO(mongoPred));
            }
        }

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
        // Get the authenticated user instead of trusting userId from request
        User authenticatedUser = userService.getCurrentAuthenticatedUser();

        Match match = matchRepository.findById(quizDTO.getMatchId())
                .orElseThrow(() -> new RuntimeException("Match not found"));

        long matchDate = match.getMatchDate();
        java.time.ZoneId istZone = java.time.ZoneId.of("Asia/Kolkata");
        java.time.LocalDate matchDay = java.time.Instant.ofEpochMilli(matchDate).atZone(istZone).toLocalDate();
        java.time.LocalDate today = java.time.LocalDate.now(istZone);

        if (!matchDay.equals(today)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Quiz is only available on the day of the match");
            return ResponseEntity.badRequest().body(error);
        }

        // Check MongoDB for existing answers using authenticated user's ID
        if (userResponseRepository.existsByUserIdAndMatchId(authenticatedUser.getId(), quizDTO.getMatchId())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "You have already submitted quiz answers for this match");
            return ResponseEntity.badRequest().body(error);
        }

        System.out.println("Quiz DTO received: userId=" + authenticatedUser.getId() + ", matchId=" + quizDTO.getMatchId() + ", answers=" + quizDTO.getAnswers());
        predictionService.saveQuizPrediction(
                authenticatedUser.getId(),
                quizDTO.getMatchId(),
                quizDTO.getAnswers()
        );
        Map<String, String> response = new HashMap<>();
        response.put("message", "Quiz submitted successfully!");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/quiz/status")
    public ResponseEntity<Map<String, Boolean>> getQuizStatus(@RequestParam Long matchId) {
        // Get the authenticated user
        User authenticatedUser = userService.getCurrentAuthenticatedUser();
        boolean exists = userResponseRepository.existsByUserIdAndMatchId(authenticatedUser.getId(), matchId);
        Map<String, Boolean> result = new HashMap<>();
        result.put("submitted", exists);
        return ResponseEntity.ok(result);
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

    @GetMapping("/invalid")
    public ResponseEntity<List<UserPredictionDTO>> getInvalidPredictions() {
        List<UserPrediction> invalidPredictions = predictionService.findInvalidPredictions();
        List<UserPredictionDTO> result = invalidPredictions.stream()
            .map(mongoPred -> new UserPredictionDTO(
                mongoPred.getUserId(),
                mongoPred.getUsername(),
                mongoPred.getPredictedWinnerName(),
                mongoPred.getMatchId()
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/invalid")
    public ResponseEntity<Map<String, Integer>> removeInvalidPredictions() {
        int removedCount = predictionService.removeInvalidPredictions();
        Map<String, Integer> response = new HashMap<>();
        response.put("removed", removedCount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/fix-userids")
    public ResponseEntity<Map<String, Integer>> fixUserIdsInPredictions() {
        int fixedCount = predictionService.fixUserIdsInPredictions();
        Map<String, Integer> response = new HashMap<>();
        response.put("fixed", fixedCount);
        return ResponseEntity.ok(response);
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

    private PredictionDTO convertFromMongoToDTO(UserPrediction mongoPred) {
        PredictionDTO dto = new PredictionDTO();
        dto.setId(null); // MongoDB id is String, not used in frontend
        dto.setUserId(mongoPred.getUserId());
        dto.setUsername(mongoPred.getUsername());
        dto.setMatchId(mongoPred.getMatchId());
        dto.setPredictedWinnerId(mongoPred.getPredictedWinnerId());
        dto.setPredictedWinnerName(mongoPred.getPredictedWinnerName());
        dto.setIsCorrect(mongoPred.getIsCorrect());
        dto.setPointsEarned(mongoPred.getPointsEarned());
        dto.setCreatedAt(mongoPred.getCreatedAt());
        dto.setHomeProbability(mongoPred.getHomeProbability());
        dto.setAwayProbability(mongoPred.getAwayProbability());
        return dto;
    }
}