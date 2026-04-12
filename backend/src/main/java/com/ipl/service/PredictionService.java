package com.ipl.service;

import com.ipl.config.PointsConfig;
import com.ipl.model.Match;
import com.ipl.model.Prediction;
import com.ipl.model.QuizAnswer;
import com.ipl.model.Team;
import com.ipl.model.User;
import com.ipl.model.mongo.UserPrediction;
import com.ipl.model.mongo.UserResponse;
import com.ipl.repository.MatchRepository;
import com.ipl.repository.PredictionRepository;
import com.ipl.repository.QuizAnswerRepository;
import com.ipl.repository.TeamRepository;
import com.ipl.repository.UserRepository;
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

@Service
@RequiredArgsConstructor
public class PredictionService {
    
    private final PredictionRepository predictionRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
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
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        
        if ("COMPLETED".equals(match.getMatchStatus())) {
            throw new RuntimeException("Cannot make predictions for a completed match");
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
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        
        if (match.getWinnerTeam() == null) {
            throw new RuntimeException("Match winner not determined");
        }
        
        List<Prediction> predictions = predictionRepository.findMatchPredictions(matchId);
        Team winner = match.getWinnerTeam();
        
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
            
            if (isCorrect) {
                userService.updateUserPoints(prediction.getUser().getId(), pointsEarned);
            }
        }
    }
    
    @Transactional
    public void resetPredictions(Long matchId) {
        List<Prediction> predictions = predictionRepository.findMatchPredictions(matchId);
        
        for (Prediction prediction : predictions) {
            Long userId = prediction.getUser().getId();
            if (prediction.getIsCorrect() != null && prediction.getIsCorrect()) {
                if (prediction.getPointsEarned() != null && prediction.getPointsEarned() > 0) {
                    userService.updateUserPoints(userId, -prediction.getPointsEarned());
                    userPointsService.updateUserPoints(userId, -prediction.getPointsEarned());
                }
            }
            prediction.setIsCorrect(false);
            prediction.setPointsEarned(0);
            predictionRepository.save(prediction);
            
            deleteUserPredictionFromMongo(userId, matchId);
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
    
    @Transactional
    public void saveQuizPrediction(Long userId, Long matchId, java.util.Map<String, String> answers) {
        try {
            System.out.println("saveQuizPrediction called with: userId=" + userId + ", matchId=" + matchId + ", answers=" + answers);
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Match match = matchRepository.findById(matchId)
                    .orElseThrow(() -> new RuntimeException("Match not found"));
            
            List<UserResponse.QuestionResponse> mongoResponses = new ArrayList<>();
            
            if (answers == null || answers.isEmpty()) {
                System.err.println("ERROR: answers map is empty or null!");
                throw new RuntimeException("Answers map is empty");
            }
            
            for (java.util.Map.Entry<String, String> entry : answers.entrySet()) {
                String questionId = entry.getKey();
                String answer = entry.getValue();
                
                System.out.println("Saving quiz answer - questionId: " + questionId + ", answer: " + answer);
                
                QuizAnswer quizAnswer = new QuizAnswer();
                quizAnswer.setUser(user);
                quizAnswer.setMatch(match);
                quizAnswer.setQuestionId(questionId);
                quizAnswer.setSelectedOption(answer);
                quizAnswer.setIsCorrect(false);
                quizAnswer.setPointsEarned(0);
                quizAnswer.setCreatedAt(System.currentTimeMillis());
                
                quizAnswerRepository.save(quizAnswer);
                
                UserResponse.QuestionResponse qr = new UserResponse.QuestionResponse();
                qr.setQuestionId(questionId);
                qr.setSelectedOption(answer);
                qr.setIsCorrect(false);
                qr.setPointsEarned(0);
                mongoResponses.add(qr);
            }
            
            UserResponse userResponse = new UserResponse();
            userResponse.setUserId(userId);
            userResponse.setUsername(user.getUsername());
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