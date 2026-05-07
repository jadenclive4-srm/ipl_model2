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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PredictionService {

    @Autowired
    private PredictionRepository predictionRepository;
    @Autowired
    private MatchRepository matchRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserMongoRepository userMongoRepository;
    @Autowired
    private UserPredictionRepository userPredictionRepository;
    @Autowired
    private QuizAnswerRepository quizAnswerRepository;
    @Autowired
    private UserResponseRepository userResponseRepository;
    @Autowired
    @Lazy
    private UserService userService;
    @Autowired
    @Lazy
    private UserPointsService userPointsService;
    
    /**
     * Get the expected username for a given userId
     */
    private String getExpectedUsernameForUserId(Long userId) {
        try {
            // Try MongoDB first (canonical source)
            Optional<com.ipl.model.mongo.UserMongo> mongoUser = userMongoRepository.findById(userId);
            if (mongoUser.isPresent()) {
                return mongoUser.get().getUsername();
            }

            // Fallback to H2
            Optional<com.ipl.model.User> h2User = userRepository.findById(userId);
            if (h2User.isPresent()) {
                return h2User.get().getUsername();
            }
        } catch (Exception e) {
            System.err.println("Error getting username for userId " + userId + ": " + e.getMessage());
        }
        return null;
    }

    @Transactional
    public Prediction createPrediction(Long userId, Long matchId, Long predictedWinnerId, Integer homeProbability, Integer awayProbability) {
        // Validate userId exists
        if (!userService.isValidUserId(userId)) {
            throw new RuntimeException("Invalid userId: " + userId + " - User does not exist in database");
        }

        // Log prediction creation details for debugging
        System.out.println("=== PREDICTION CREATION DEBUG ===");
        System.out.println("userId: " + userId);
        System.out.println("matchId: " + matchId);
        System.out.println("predictedWinnerId: " + predictedWinnerId);
        System.out.println("homeProbability: " + homeProbability);
        System.out.println("awayProbability: " + awayProbability);
        System.out.println("Current authenticated user: " + (userService.getCurrentAuthenticatedUser() != null ?
            userService.getCurrentAuthenticatedUser().getUsername() : "null"));
        System.out.println("===================================");

        // Check for existing prediction in MongoDB (primary storage)
        boolean mongoExists = userPredictionRepository.existsByUserIdAndMatchId(userId, matchId);
        System.out.println("MongoDB prediction exists for user " + userId + " and match " + matchId + ": " + mongoExists);
        if (mongoExists) {
            throw new RuntimeException("Prediction already exists for this match");
        }

        // Also check H2 for backward compatibility (though currently disabled)
        boolean h2Exists = predictionRepository.existsByUserIdAndMatchId(userId, matchId);
        System.out.println("H2 prediction exists for user " + userId + " and match " + matchId + ": " + h2Exists);
        if (h2Exists) {
            throw new RuntimeException("Prediction already exists for this match");
        }
        
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        System.out.println("Match details: id=" + match.getId() + ", status=" + match.getMatchStatus() +
            ", matchDate=" + match.getMatchDate() + ", currentTime=" + System.currentTimeMillis());

        if ("COMPLETED".equals(match.getMatchStatus())) {
            throw new RuntimeException("Cannot make predictions for a completed match");
        }

        long predictionCloseTime = match.getMatchDate() - (30 * 60 * 1000);
        long currentTime = System.currentTimeMillis();
        System.out.println("Prediction window check: matchDate=" + match.getMatchDate() +
            ", predictionCloseTime=" + predictionCloseTime + ", currentTime=" + currentTime +
            ", windowClosed=" + (currentTime >= predictionCloseTime));
        if (currentTime >= predictionCloseTime) {
            throw new RuntimeException("Prediction window has closed for this match");
        }
        
        Team predictedWinner = null;
        if (predictedWinnerId != null) {
            predictedWinner = teamRepository.findById(predictedWinnerId)
                    .orElseThrow(() -> new RuntimeException("Team not found"));
            System.out.println("Predicted winner team: " + predictedWinner.getTeamName());
        } else {
            System.out.println("No predicted winner specified (predictedWinnerId is null)");
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
        
        // Temporarily disable H2 storage to test if bug is related to H2
        // Prediction saved = predictionRepository.save(prediction);

        // Ensure we use the authenticated user's username, not any username from request
        String authenticatedUsername = user.getUsername();
        System.out.println("Storing prediction for authenticated user: userId=" + userId + ", username='" + authenticatedUsername + "'");

        saveUserPredictionToMongo(userId, authenticatedUsername, matchId, predictedWinnerId,
                predictedWinner != null ? predictedWinner.getTeamName() : null,
                homeProbability, awayProbability);

        // Return a dummy prediction object for now
        Prediction dummyPrediction = new Prediction();
        dummyPrediction.setUser(user);
        dummyPrediction.setMatch(match);
        dummyPrediction.setPredictedWinner(predictedWinner);
        dummyPrediction.setHomeProbability(homeProbability);
        dummyPrediction.setAwayProbability(awayProbability);
        dummyPrediction.setIsCorrect(false);
        dummyPrediction.setPointsEarned(0);
        dummyPrediction.setCreatedAt(System.currentTimeMillis());
        dummyPrediction.setId(-1L); // Dummy ID

        return dummyPrediction;
    }
    
    public List<Prediction> getUserPredictions(Long userId) {
        return predictionRepository.findUserPredictions(userId);
    }
    
    public List<Prediction> getMatchPredictions(Long matchId) {
        // Temporarily return empty list since H2 storage is disabled
        return new ArrayList<>();
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

        // Temporarily only evaluate MongoDB predictions since H2 storage is disabled
        List<UserPrediction> mongoPredictions = userPredictionRepository.findByMatchId(matchId);
        Team winner = match.getWinnerTeam();

        for (UserPrediction mongoPred : mongoPredictions) {
            // Skip if already evaluated to prevent double evaluation
            if (mongoPred.getIsCorrect() != null) {
                System.out.println("Skipping already evaluated prediction for user " + mongoPred.getUserId() +
                    " (isCorrect=" + mongoPred.getIsCorrect() + ")");
                continue;
            }

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
    
    @Transactional
    public void resetPredictions(Long matchId) {
        // Temporarily only reset MongoDB predictions since H2 storage is disabled
        List<UserPrediction> mongoPredictions = userPredictionRepository.findByMatchId(matchId);
        for (UserPrediction mongoPred : mongoPredictions) {
            Long userId = mongoPred.getUserId();
            if (mongoPred.getIsCorrect() != null && mongoPred.getIsCorrect()) {
                if (mongoPred.getPointsEarned() != null && mongoPred.getPointsEarned() > 0) {
                    userPointsService.updateUserPoints(userId, -mongoPred.getPointsEarned());
                }
            }
            mongoPred.setIsCorrect(null); // Reset to not evaluated state
            mongoPred.setPointsEarned(null); // Reset to not evaluated state
            userPredictionRepository.save(mongoPred);
        }
    }

    // Method to fix existing predictions that have incorrect default values
    public void fixExistingPredictionDefaults() {
        List<UserPrediction> allPredictions = userPredictionRepository.findAll();
        int fixedCount = 0;

        for (UserPrediction pred : allPredictions) {
            // If prediction has isCorrect = false and pointsEarned = 0, it's likely unevaluated
            // (since evaluated incorrect predictions get pointsEarned = 0, but evaluated correct get > 0)
            // However, we can't reliably distinguish, so let's be conservative and only fix
            // predictions that have both isCorrect = false AND pointsEarned = 0 AND no evaluation indicators

            // For now, let's fix predictions that have the exact initial state
            if (pred.getIsCorrect() != null && pred.getIsCorrect() == false &&
                pred.getPointsEarned() != null && pred.getPointsEarned() == 0) {
                // This might be an unevaluated prediction, set to null
                pred.setIsCorrect(null);
                pred.setPointsEarned(null);
                userPredictionRepository.save(pred);
                fixedCount++;
            }
        }

        System.out.println("Fixed " + fixedCount + " existing predictions to use null defaults");
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
            // Validate that username matches the userId
            String expectedUsername = getExpectedUsernameForUserId(userId);
            if (expectedUsername != null && !expectedUsername.equals(username)) {
                System.err.println("WARNING: Username mismatch! userId=" + userId +
                    " provided username='" + username + "' but expected='" + expectedUsername + "'");
                System.err.println("Using correct username: '" + expectedUsername + "'");
                username = expectedUsername; // Override with correct username
            }

            System.out.println("=== MONGODB SAVE DEBUG ===");
            System.out.println("userId: " + userId);
            System.out.println("username: " + username + " (validated)");
            System.out.println("matchId: " + matchId);
            System.out.println("predictedWinnerId: " + predictedWinnerId);
            System.out.println("predictedWinnerName: " + predictedWinnerName);
            System.out.println("homeProbability: " + homeProbability);
            System.out.println("awayProbability: " + awayProbability);
            System.out.println("===========================");
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
                userPrediction.setIsCorrect(null); // null indicates not evaluated yet
                userPrediction.setPointsEarned(null); // null indicates not evaluated yet
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

    /**
     * Fix userIds in MongoDB predictions based on username
     */
    public int fixUserIdsInPredictions() {
        List<UserPrediction> allPredictions = userPredictionRepository.findAll();
        int fixedCount = 0;
        for (UserPrediction pred : allPredictions) {
            try {
                User user = userService.findByUsername(pred.getUsername()).orElse(null);
                if (user != null && !user.getId().equals(pred.getUserId())) {
                    System.out.println("Fixing userId for prediction: username=" + pred.getUsername() +
                        ", old userId=" + pred.getUserId() + ", new userId=" + user.getId());
                    pred.setUserId(user.getId());
                    userPredictionRepository.save(pred);
                    fixedCount++;
                }
            } catch (Exception e) {
                System.err.println("Error fixing userId for prediction with username " + pred.getUsername() + ": " + e.getMessage());
            }
        }
        System.out.println("Fixed userIds for " + fixedCount + " predictions");
        return fixedCount;
    }

    public int fixUserIdsInQuizResponses() {
        List<UserResponse> allResponses = userResponseRepository.findAll();
        int fixedCount = 0;
        for (UserResponse response : allResponses) {
            try {
                User user = userService.findByUsername(response.getUsername()).orElse(null);
                if (user != null && !user.getId().equals(response.getUserId())) {
                    System.out.println("Fixing userId for quiz response: username=" + response.getUsername() +
                        ", old userId=" + response.getUserId() + ", new userId=" + user.getId());
                    response.setUserId(user.getId());
                    userResponseRepository.save(response);
                    fixedCount++;
                }
            } catch (Exception e) {
                System.err.println("Error fixing userId for quiz response with username " + response.getUsername() + ": " + e.getMessage());
            }
        }
        System.out.println("Fixed userIds for " + fixedCount + " quiz responses");
        return fixedCount;
    }

    @Transactional
    public void saveQuizPrediction(String username, Long matchId, java.util.Map<String, String> answers) {
        try {
            System.out.println("saveQuizPrediction called with: username=" + username + ", matchId=" + matchId + ", answers=" + answers);

            // Find user by username to get the correct userId
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Long userId = user.getId();

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
                qr.setPointsEarned(null); // null indicates not evaluated yet
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

    public Map<Long, Integer> getPredictionCountsByTeamForTodaysMatches() {
        // Get today's date in IST
        java.time.ZoneId istZone = java.time.ZoneId.of("Asia/Kolkata");
        java.time.LocalDate today = java.time.LocalDate.now(istZone);
        long startOfDay = today.atStartOfDay(istZone).toInstant().toEpochMilli();
        long endOfDay = today.atTime(java.time.LocalTime.MAX).atZone(istZone).toInstant().toEpochMilli();

        // Get matches scheduled for today
        List<Match> todaysMatches = matchRepository.findMatchesForToday(startOfDay, endOfDay);
        List<Long> todaysMatchIds = todaysMatches.stream().map(Match::getId).collect(Collectors.toList());

        if (todaysMatchIds.isEmpty()) {
            System.out.println("No matches today");
            return new HashMap<>();
        }

        // Get predictions for today's matches
        List<UserPrediction> todaysPredictions = new ArrayList<>();
        for (Long matchId : todaysMatchIds) {
            todaysPredictions.addAll(userPredictionRepository.findByMatchId(matchId));
        }

        System.out.println("Today's matches: " + todaysMatchIds.size() + ", predictions: " + todaysPredictions.size());
        Map<Long, Integer> counts = new HashMap<>();
        for (UserPrediction pred : todaysPredictions) {
            Long teamId = pred.getPredictedWinnerId();
            if (teamId != null) {
                counts.put(teamId, counts.getOrDefault(teamId, 0) + 1);
            }
        }
        System.out.println("Today's vote counts for matches: " + counts);
        return counts;
    }

    public Map<String, Long> getOverallPredictionAccuracy() {
        List<UserPrediction> allPredictions = userPredictionRepository.findAll();
        long correct = allPredictions.stream()
            .filter(pred -> pred.getIsCorrect() != null && pred.getIsCorrect())
            .count();
        long total = allPredictions.size();

        System.out.println("Overall prediction accuracy: correct=" + correct + ", total=" + total);
        return Map.of("correct", correct, "total", total);
    }
}