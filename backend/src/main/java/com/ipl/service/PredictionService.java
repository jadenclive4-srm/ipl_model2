package com.ipl.service;

import com.ipl.config.PointsConfig;
import com.ipl.model.Match;
import com.ipl.model.Prediction;
import com.ipl.model.QuizAnswer;
import com.ipl.model.Team;
import com.ipl.model.User;
import com.ipl.model.mongo.UserMongo;
import com.ipl.model.mongo.UserPrediction;
import com.ipl.model.mongo.UserResponse;
import com.ipl.repository.MatchRepository;
import com.ipl.repository.PredictionRepository;
import com.ipl.repository.QuizAnswerRepository;
import com.ipl.repository.TeamRepository;
import com.ipl.repository.UserRepository;
import com.ipl.repository.mongo.UserMongoRepository;
import com.ipl.repository.mongo.UserPredictionRepository;
import com.ipl.repository.mongo.UserResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PredictionService {
    
    private final PredictionRepository predictionRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final UserMongoRepository userMongoRepository;
    private final UserPredictionRepository userPredictionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final UserResponseRepository userResponseRepository;
    @Autowired
    @Lazy
    private UserService userService;
    @Autowired
    @Lazy
    private UserPointsService userPointsService;
    
    @Transactional
    public Prediction createPrediction(Long userId, Long matchId, Long predictedWinnerId, Integer homeProbability, Integer awayProbability) {
        if (predictionRepository.existsByUserIdAndMatchId(userId, matchId)) {
            throw new RuntimeException("Prediction already exists for this match");
        }
        
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        
        if ("COMPLETED".equals(match.getMatchStatus())) {
            throw new RuntimeException("Cannot make predictions for a completed match");
        }
        
        long predictionCloseTime = match.getMatchDate() - (30 * 60 * 1000);
        if (System.currentTimeMillis() >= predictionCloseTime) {
            throw new RuntimeException("Prediction window has closed for this match");
        }
        
        Team predictedWinner = null;
        if (predictedWinnerId != null) {
            predictedWinner = teamRepository.findById(predictedWinnerId)
                    .orElseThrow(() -> new RuntimeException("Team not found"));
        }
        
        Prediction prediction = new Prediction();
        prediction.setUser(user);
        prediction.setMatch(match);
        prediction.setPredictedWinner(predictedWinner);
        prediction.setHomeProbability(homeProbability);
        prediction.setAwayProbability(awayProbability);
        prediction.setIsCorrect(false);
        prediction.setPointsEarned(0);
        prediction.setCreatedAt(System.currentTimeMillis());
        
        Prediction saved = predictionRepository.save(prediction);
        
        saveUserPredictionToMongo(userId, user.getUsername(), matchId, predictedWinnerId, 
                predictedWinner != null ? predictedWinner.getTeamName() : null, 
                homeProbability, awayProbability);
        
        return saved;
    }
    
    public List<Prediction> getUserPredictions(Long userId) {
        return predictionRepository.findUserPredictions(userId);
    }
    
    public List<Prediction> getMatchPredictions(Long matchId) {
        return predictionRepository.findMatchPredictions(matchId);
    }
    
    public Optional<Prediction> getUserMatchPrediction(Long userId, Long matchId) {
        return predictionRepository.findByUserIdAndMatchId(userId, matchId);
    }
    
    @Transactional
    public void evaluatePredictions(Long matchId) {
        // Validation is done in controller, so match should exist and have winner
        Match match = matchRepository.findById(matchId).orElse(null);
        if (match == null || match.getWinnerTeam() == null) {
            // This shouldn't happen if validation is correct, but handle gracefully
            System.err.println("evaluatePredictions called with invalid match: " + matchId);
            return;
        }

        List<Prediction> predictions = predictionRepository.findMatchPredictions(matchId);
        Team winner = match.getWinnerTeam();

        // Evaluate H2 predictions
        for (Prediction prediction : predictions) {
            boolean isCorrect = false;
            int pointsEarned = 0;

            if (prediction.getPredictedWinner() != null &&
                prediction.getPredictedWinner().getId().equals(winner.getId())) {
                isCorrect = true;
                pointsEarned = calculatePoints(prediction.getHomeProbability(), prediction.getAwayProbability());
            }

            prediction.setIsCorrect(isCorrect);
            prediction.setPointsEarned(pointsEarned);
            predictionRepository.save(prediction);

            if (pointsEarned > 0) {
                userPointsService.updateUserPoints(prediction.getUser().getId(), pointsEarned);
            }

            updateUserPredictionInMongo(prediction.getUser().getId(), matchId, isCorrect, pointsEarned);
        }

        // Evaluate MongoDB predictions (for admin-created users)
        List<UserPrediction> mongoPredictions = userPredictionRepository.findByMatchId(matchId);
        for (UserPrediction mongoPred : mongoPredictions) {
            // Skip if this prediction was already evaluated above (has corresponding H2 prediction)
            boolean alreadyEvaluated = predictions.stream()
                    .anyMatch(h2Pred -> h2Pred.getUser().getId().equals(mongoPred.getUserId()));

            if (!alreadyEvaluated) {
                boolean isCorrect = false;
                int pointsEarned = 0;

                if (mongoPred.getPredictedWinnerId() != null &&
                    mongoPred.getPredictedWinnerId().equals(winner.getId())) {
                    isCorrect = true;
                    pointsEarned = calculatePoints(mongoPred.getHomeProbability(), mongoPred.getAwayProbability());
                }

                mongoPred.setIsCorrect(isCorrect);
                mongoPred.setPointsEarned(pointsEarned);
                userPredictionRepository.save(mongoPred);

                if (pointsEarned > 0) {
                    userPointsService.updateUserPoints(mongoPred.getUserId(), pointsEarned);
                }

                System.out.println("Evaluated MongoDB prediction for user " + mongoPred.getUserId() +
                    ": correct=" + isCorrect + ", points=" + pointsEarned);
            }
        }
    }
    
    @Transactional
    public void resetPredictions(Long matchId) {
        // Reset H2 predictions
        List<Prediction> predictions = predictionRepository.findMatchPredictions(matchId);

        for (Prediction prediction : predictions) {
            Long userId = prediction.getUser().getId();
            if (prediction.getIsCorrect() != null && prediction.getIsCorrect()) {
                if (prediction.getPointsEarned() != null && prediction.getPointsEarned() > 0) {
                    userPointsService.updateUserPoints(userId, -prediction.getPointsEarned());
                }
            }
            prediction.setIsCorrect(false);
            prediction.setPointsEarned(0);
            predictionRepository.save(prediction);

            deleteUserPredictionFromMongo(userId, matchId);
        }

        // Reset MongoDB predictions (for admin-created users)
        List<UserPrediction> mongoPredictions = userPredictionRepository.findByMatchId(matchId);
        for (UserPrediction mongoPred : mongoPredictions) {
            Long userId = mongoPred.getUserId();
            if (mongoPred.getIsCorrect() != null && mongoPred.getIsCorrect()) {
                if (mongoPred.getPointsEarned() != null && mongoPred.getPointsEarned() > 0) {
                    userPointsService.updateUserPoints(userId, -mongoPred.getPointsEarned());
                }
            }
            mongoPred.setIsCorrect(false);
            mongoPred.setPointsEarned(0);
            userPredictionRepository.save(mongoPred);

            System.out.println("Reset MongoDB prediction for user " + userId + " on match " + matchId);
        }
    }
    
    private void deleteUserPredictionFromMongo(Long userId, Long matchId) {
        try {
            Optional<UserPrediction> existing = userPredictionRepository.findByUserIdAndMatchId(userId, matchId);
            existing.ifPresent(up -> {
                userPredictionRepository.delete(up);
            });
        } catch (Exception e) {
            System.err.println("MongoDB delete error (non-fatal): " + e.getMessage());
        }
    }
    
    @Transactional
    public void deleteAllPredictionsForMatch(Long matchId, Long userId) {
        Optional<Prediction> pred = predictionRepository.findByUserIdAndMatchId(userId, matchId);
        pred.ifPresent(p -> predictionRepository.delete(p));
    }
    
    @Transactional
    public void deleteAllPredictionsForMatchV2(Long matchId) {
        predictionRepository.deleteAllByMatchIdNative(matchId);
    }
    
    public void deleteAllUserPredictionsFromMongo(Long matchId) {
        List<UserPrediction> mongoPreds = userPredictionRepository.findByMatchId(matchId);
        if (!mongoPreds.isEmpty()) {
            userPredictionRepository.deleteAll(mongoPreds);
        }
    }
    
    private int calculatePoints(Integer homeProbability, Integer awayProbability) {
        int confidence = Math.max(homeProbability != null ? homeProbability : 50, 
                                awayProbability != null ? awayProbability : 50);
        
        if (confidence >= 80) {
            return PointsConfig.PREDICTION_CORRECT * 2;
        } else if (confidence >= 60) {
            return (int) (PointsConfig.PREDICTION_CORRECT * 1.5);
        } else {
            return PointsConfig.PREDICTION_CORRECT;
        }
    }
    
    public Long getCorrectPredictionCount(Long userId) {
        return predictionRepository.countCorrectPredictions(userId);
    }
    
    public Long getTotalPoints(Long userId) {
        Long points = predictionRepository.sumPointsByUser(userId);
        return points != null ? points : 0L;
    }
    
    private void saveUserPredictionToMongo(Long userId, String username, Long matchId, 
            Long predictedWinnerId, String predictedWinnerName, Integer homeProbability, Integer awayProbability) {
        try {
            System.out.println("MongoDB: Saving prediction for user=" + userId + " match=" + matchId);
            System.out.println("MongoDB: Attempting to connect and save...");
            Optional<UserPrediction> existing = userPredictionRepository.findByUserIdAndMatchId(userId, matchId);
            
            UserPrediction userPrediction;
            if (existing.isPresent()) {
                userPrediction = existing.get();
                userPrediction.setPredictedWinnerId(predictedWinnerId);
                userPrediction.setPredictedWinnerName(predictedWinnerName);
                userPrediction.setHomeProbability(homeProbability);
                userPrediction.setAwayProbability(awayProbability);
            } else {
                userPrediction = new UserPrediction();
                userPrediction.setUserId(userId);
                userPrediction.setUsername(username);
                userPrediction.setMatchId(matchId);
                userPrediction.setPredictedWinnerId(predictedWinnerId);
                userPrediction.setPredictedWinnerName(predictedWinnerName);
                userPrediction.setHomeProbability(homeProbability);
                userPrediction.setAwayProbability(awayProbability);
                userPrediction.setIsCorrect(false);
                userPrediction.setPointsEarned(0);
                userPrediction.setCreatedAt(System.currentTimeMillis());
            }
            
            userPredictionRepository.save(userPrediction);
            System.out.println("MongoDB: Prediction saved successfully to collection user_predictions");
        } catch (Exception e) {
            System.err.println("MongoDB save error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("Authentication")) {
                System.err.println("MongoDB auth failed - check username/password in connection string");
            }
        }
    }
    
    private void updateUserPredictionInMongo(Long userId, Long matchId, Boolean isCorrect, Integer pointsEarned) {
        try {
            Optional<UserPrediction> existing = userPredictionRepository.findByUserIdAndMatchId(userId, matchId);
            existing.ifPresent(up -> {
                up.setIsCorrect(isCorrect);
                up.setPointsEarned(pointsEarned);
                userPredictionRepository.save(up);
            });
        } catch (Exception e) {
            System.err.println("MongoDB update error (non-fatal): " + e.getMessage());
        }
    }

    /**
     * Find predictions in MongoDB that have invalid userIds (user doesn't exist in H2 or MongoDB)
     */
    public List<UserPrediction> findInvalidPredictions() {
        List<UserPrediction> allPredictions = userPredictionRepository.findAll();
        return allPredictions.stream()
                .filter(pred -> {
                    // Check if user exists in H2
                    boolean existsInH2 = userRepository.existsById(pred.getUserId());
                    if (existsInH2) return false;

                    // Check if user exists in MongoDB
                    try {
                        boolean existsInMongo = userMongoRepository.existsById(pred.getUserId());
                        return !existsInMongo;
                    } catch (Exception e) {
                        System.err.println("Error checking MongoDB for user " + pred.getUserId() + ": " + e.getMessage());
                        return true; // Consider invalid if we can't check
                    }
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Remove all predictions with invalid userIds from MongoDB
     */
    public int removeInvalidPredictions() {
        List<UserPrediction> invalidPredictions = findInvalidPredictions();
        if (!invalidPredictions.isEmpty()) {
            userPredictionRepository.deleteAll(invalidPredictions);
            System.out.println("Removed " + invalidPredictions.size() + " invalid predictions from MongoDB");
        }
        return invalidPredictions.size();
    }
    
    @Transactional
    public void saveQuizPrediction(Long userId, Long matchId, java.util.Map<String, String> answers) {
        try {
            System.out.println("saveQuizPrediction called with: userId=" + userId + ", matchId=" + matchId + ", answers=" + answers);

            // Try to find user in H2 first, then MongoDB
            String username;
            try {
                User user = userService.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                username = user.getUsername();
            } catch (Exception e) {
                // If H2 user not found, check MongoDB directly
                try {
                    Optional<UserMongo> mongoUser = userMongoRepository.findById(userId);
                    if (mongoUser.isPresent()) {
                        username = mongoUser.get().getUsername();
                    } else {
                        throw new RuntimeException("User not found in both H2 and MongoDB");
                    }
                } catch (Exception mongoEx) {
                    System.err.println("Error checking MongoDB for user: " + mongoEx.getMessage());
                    throw new RuntimeException("User not found: " + e.getMessage());
                }
            }

            // Check if already submitted
            if (userResponseRepository.existsByUserIdAndMatchId(userId, matchId)) {
                throw new RuntimeException("You have already submitted quiz answers for this match");
            }

            List<UserResponse.QuestionResponse> mongoResponses = new ArrayList<>();

            if (answers == null || answers.isEmpty()) {
                System.err.println("ERROR: answers map is empty or null!");
                throw new RuntimeException("Answers map is empty");
            }

            for (java.util.Map.Entry<String, String> entry : answers.entrySet()) {
                String questionId = entry.getKey();
                String answer = entry.getValue();

                System.out.println("Saving quiz answer - questionId: " + questionId + ", answer: " + answer);

                UserResponse.QuestionResponse qr = new UserResponse.QuestionResponse();
                qr.setQuestionId(questionId);
                qr.setSelectedOption(answer);
                qr.setIsCorrect(false);
                qr.setPointsEarned(0);
                mongoResponses.add(qr);
            }

            UserResponse userResponse = new UserResponse();
            userResponse.setUserId(userId);
            userResponse.setUsername(username);
            userResponse.setMatchId(matchId);
            userResponse.setResponses(mongoResponses);
            userResponse.setCreatedAt(System.currentTimeMillis());

            System.out.println("UserResponse object: " + userResponse);
            System.out.println("Responses list size: " + mongoResponses.size());
            for (int i = 0; i < mongoResponses.size(); i++) {
                System.out.println("Response " + i + ": " + mongoResponses.get(i));
            }

            UserResponse saved = userResponseRepository.save(userResponse);
            System.out.println("Saved UserResponse with id: " + saved.getId());

            System.out.println("Quiz answers saved successfully for user: " + userId + ", match: " + matchId);
        } catch (Exception e) {
            System.err.println("Error saving quiz prediction: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save quiz prediction: " + e.getMessage(), e);
        }
    }
}