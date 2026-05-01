package com.ipl.service;

import com.ipl.config.PointsConfig;
import com.ipl.dto.QuestionDTO;
import com.ipl.dto.UserAnswerDTO;
import com.ipl.model.Question;
import com.ipl.model.User;
import com.ipl.model.UserAnswer;
import com.ipl.model.Match;
import com.ipl.model.mongo.QuizCorrectAnswers;
import com.ipl.model.mongo.UserResponse;
import com.ipl.repository.QuestionRepository;
import com.ipl.repository.UserAnswerRepository;
import com.ipl.repository.UserRepository;
import com.ipl.repository.MatchRepository;
import com.ipl.repository.mongo.QuizCorrectAnswersRepository;
import com.ipl.repository.mongo.UserMongoRepository;
import com.ipl.repository.mongo.UserResponseRepository;
import com.ipl.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuestionService {
    
    private final QuestionRepository questionRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final UserRepository userRepository;
    private final UserMongoRepository userMongoRepository;
    private final UserService userService;
    private final MatchRepository matchRepository;
    private final UserResponseRepository userResponseRepository;
    private final QuizCorrectAnswersRepository quizCorrectAnswersRepository;
    private final UserPointsService userPointsService;
    
    public List<QuestionDTO> getQuestionsByMatchId(Long matchId, Long userId) {
        List<Question> questions = questionRepository.findActiveQuestionsByMatchId(matchId);
        List<QuestionDTO> questionDTOs = new ArrayList<>();
        
        for (Question question : questions) {
            QuestionDTO dto = convertToDTO(question);
            
            Optional<UserAnswer> existingAnswer = userAnswerRepository.findByUserIdAndQuestionId(userId, question.getId());
            if (existingAnswer.isPresent()) {
                dto.setHasAnswered(true);
                dto.setUserAnswer(existingAnswer.get().getSelectedOption());
            } else {
                dto.setHasAnswered(false);
            }
            
            questionDTOs.add(dto);
        }
        
        return questionDTOs;
    }
    
    @Transactional
    public UserAnswerDTO submitAnswer(Long userId, Long questionId, String selectedOption) {
        // Try to find user in H2 first, then MongoDB
        User user;
        try {
            user = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } catch (Exception e) {
            // If H2 user not found, create a minimal User object from MongoDB
            try {
                Optional<com.ipl.model.mongo.UserMongo> mongoUser = userMongoRepository.findById(userId);
                if (mongoUser.isPresent()) {
                    com.ipl.model.mongo.UserMongo um = mongoUser.get();
                    user = new User();
                    user.setId(um.getId());
                    user.setUsername(um.getUsername());
                    user.setEmail(um.getEmail());
                    user.setFullName(um.getFullName());
                    user.setRole(um.getRole());
                } else {
                    throw new RuntimeException("User not found in both H2 and MongoDB");
                }
            } catch (Exception mongoEx) {
                System.err.println("Error checking MongoDB for user: " + mongoEx.getMessage());
                throw new RuntimeException("User not found: " + e.getMessage());
            }
        }
        
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        
        Optional<UserAnswer> existingAnswer = userAnswerRepository.findByUserIdAndQuestionId(userId, questionId);
        
        UserAnswer userAnswer;
        if (existingAnswer.isPresent()) {
            userAnswer = existingAnswer.get();
            userAnswer.setSelectedOption(selectedOption);
        } else {
            userAnswer = new UserAnswer();
            userAnswer.setUser(user);
            userAnswer.setQuestion(question);
            userAnswer.setSelectedOption(selectedOption);
            userAnswer.setAnsweredAt(System.currentTimeMillis());
        }
        
        boolean isCorrect = selectedOption.equalsIgnoreCase(question.getCorrectOption());
        userAnswer.setIsCorrect(isCorrect);
        
        if (isCorrect) {
            userAnswer.setPointsEarned(PointsConfig.QUIZ_CORRECT);
        } else {
            userAnswer.setPointsEarned(0);
        }
        
        userAnswer = userAnswerRepository.save(userAnswer);
        
        if (isCorrect) {
            userPointsService.updateUserPoints(userId, PointsConfig.QUIZ_CORRECT);
        }
        
        return convertAnswerToDTO(userAnswer);
    }
    
    @Transactional
    public List<UserAnswerDTO> submitAnswers(Long userId, Long matchId, List<String> answers, List<Long> questionIds) {
        // Try to find user in H2 first, then MongoDB
        String username;
        try {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            username = user.getUsername();
        } catch (Exception e) {
            // If H2 user not found, check MongoDB directly
            try {
                Optional<com.ipl.model.mongo.UserMongo> mongoUser = userMongoRepository.findById(userId);
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

        List<UserAnswerDTO> result = new ArrayList<>();
        List<UserResponse.QuestionResponse> questionResponses = new ArrayList<>();

        for (int i = 0; i < questionIds.size(); i++) {
            UserAnswerDTO answer = submitAnswer(userId, questionIds.get(i), answers.get(i));
            result.add(answer);

            UserResponse.QuestionResponse qr = new UserResponse.QuestionResponse();
            qr.setQuestionId(String.valueOf(questionIds.get(i)));
            qr.setSelectedOption(answers.get(i));
            qr.setIsCorrect(answer.getIsCorrect());
            qr.setPointsEarned(answer.getPointsEarned());
            questionResponses.add(qr);
        }

        saveUserResponseToMongo(userId, username, matchId, questionResponses);

        return result;
    }
    
    private void saveUserResponseToMongo(Long userId, String username, Long matchId, List<UserResponse.QuestionResponse> responses) {
        Optional<UserResponse> existing = userResponseRepository.findByUserIdAndMatchId(userId, matchId);
        
        UserResponse userResponse;
        if (existing.isPresent()) {
            userResponse = existing.get();
            userResponse.setResponses(responses);
        } else {
            userResponse = new UserResponse();
            userResponse.setUserId(userId);
            userResponse.setUsername(username);
            userResponse.setMatchId(matchId);
            userResponse.setResponses(responses);
            userResponse.setCreatedAt(System.currentTimeMillis());
        }
        
        userResponseRepository.save(userResponse);
    }
    
    public List<UserAnswerDTO> getUserAnswersByMatchId(Long userId, Long matchId) {
        List<UserAnswer> answers = userAnswerRepository.findByUserIdAndMatchId(userId, matchId);
        return answers.stream()
                .map(this::convertAnswerToDTO)
                .collect(java.util.stream.Collectors.toList());
    }
    
    @Transactional
    public Question updateQuestionCorrectOption(Long questionId, String correctOption) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        question.setCorrectOption(correctOption);
        return questionRepository.save(question);
    }
    
    public List<Question> getAllQuestionsByMatchId(Long matchId) {
        return questionRepository.findAllByMatchId(matchId);
    }
    
    @Transactional
    public void evaluateQuizAnswers(Long matchId) {
        Map<String, String> correctAnswerLetters = getCorrectAnswersFromMongo(matchId);
        if (correctAnswerLetters == null || correctAnswerLetters.isEmpty()) {
            throw new RuntimeException("Cannot evaluate quiz: correct answers not set for match " + matchId);
        }
        System.out.println("Evaluating quiz for matchId=" + matchId + ", correctAnswerLetters=" + correctAnswerLetters);
        
        List<Question> questions = questionRepository.findAllByMatchId(matchId);
        System.out.println("Found " + questions.size() + " questions in H2 for match " + matchId);
        
        Map<Long, Map<String, String>> questionIdToOptionMap = new HashMap<>();
        if (questions.isEmpty()) {
            System.out.println("WARNING: No questions in H2. Building dynamic defaults from match data.");
            Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Cannot evaluate: No questions and match not found for matchId=" + matchId));
            questionIdToOptionMap = buildDefaultQuestionOptionsForMatch(match);
        } else {
            for (Question q : questions) {
                questionIdToOptionMap.put(q.getId(), buildOptionMap(q));
            }
            // Also add default options (keys 1-5) to handle legacy user responses that may still reference old IDs
            Match match = matchRepository.findById(matchId).orElse(null);
            if (match != null) {
                Map<Long, Map<String, String>> defaults = buildDefaultQuestionOptionsForMatch(match);
                for (Map.Entry<Long, Map<String, String>> e : defaults.entrySet()) {
                    if (!questionIdToOptionMap.containsKey(e.getKey())) {
                        questionIdToOptionMap.put(e.getKey(), e.getValue());
                    }
                }
            }
        }
        System.out.println("Available question IDs: " + questionIdToOptionMap.keySet());
        
        List<UserResponse> userResponses = userResponseRepository.findByMatchId(matchId);
        System.out.println("Found " + userResponses.size() + " user quiz responses to evaluate");
        
        for (UserResponse userResponse : userResponses) {
            Long userId = userResponse.getUserId();
            List<UserResponse.QuestionResponse> responses = userResponse.getResponses();
            if (responses == null) continue;
            boolean updated = false;
            for (UserResponse.QuestionResponse qr : responses) {
                String questionIdStr = qr.getQuestionId();
                if (questionIdStr == null) continue;
                String selectedOptionText = qr.getSelectedOption();
                String correctLetter = correctAnswerLetters.get(questionIdStr);
                if (correctLetter == null) {
                    System.out.println("No correct answer defined for questionId=" + questionIdStr);
                    continue;
                }
                Long questionId = null;
                try {
                    questionId = Long.parseLong(questionIdStr);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid questionId format: " + questionIdStr);
                    continue;
                }
                Map<String, String> optionMap = questionIdToOptionMap.get(questionId);
                if (optionMap == null) {
                    System.out.println("No option map for questionId=" + questionIdStr + 
                                     ". Available: " + questionIdToOptionMap.keySet());
                    continue;
                }
                String correctOptionText = optionMap.get(correctLetter);
                if (correctOptionText == null) {
                    System.out.println("No option text for letter '" + correctLetter + "' in question " + questionId);
                    continue;
                }
                boolean isCorrect = selectedOptionText != null && 
                                   selectedOptionText.equalsIgnoreCase(correctOptionText);
                qr.setIsCorrect(isCorrect);
                if (isCorrect) {
                    qr.setPointsEarned(PointsConfig.QUIZ_CORRECT);
                    System.out.println("User " + userId + " CORRECT for Q" + questionId + 
                                       ". +" + PointsConfig.QUIZ_CORRECT + " points. Selected='" + 
                                       selectedOptionText + "', Expected='" + correctOptionText + "'");
                    try {
                        userPointsService.updateUserPoints(userId, PointsConfig.QUIZ_CORRECT);
                    } catch (Exception e) {
                        System.err.println("Points award failed for userId=" + userId + ": " + e.getMessage());
                    }
                } else {
                    qr.setPointsEarned(0);
                    System.out.println("User " + userId + " INCORRECT for Q" + questionId + 
                                       ". Selected='" + selectedOptionText + "', Expected='" + correctOptionText + "'");
                }
                updated = true;
            }
            if (updated) {
                try {
                    userResponseRepository.save(userResponse);
                    System.out.println("Saved updated UserResponse for userId=" + userId + ", matchId=" + matchId);
                } catch (Exception e) {
                    System.err.println("Failed to save UserResponse for userId=" + userId + ": " + e.getMessage());
                }
            }
        }
        System.out.println("Quiz evaluation completed for matchId=" + matchId);
    }
    
    @Transactional
    public void evaluateQuizAnswersFromMongo(Long matchId) {
        evaluateQuizAnswers(matchId);
    }
    
    @Transactional
    public void resetQuizAnswers(Long matchId) {
        List<Question> questions = questionRepository.findAllByMatchId(matchId);
        
        for (Question question : questions) {
            List<UserAnswer> answers = userAnswerRepository.findByQuestionId(question.getId());
            
            for (UserAnswer answer : answers) {
                Long userId = answer.getUser().getId();
                if (answer.getIsCorrect() != null && answer.getIsCorrect()) {
                    if (answer.getPointsEarned() != null && answer.getPointsEarned() > 0) {
                        userPointsService.updateUserPoints(userId, -answer.getPointsEarned());
                    }
                }
                answer.setIsCorrect(false);
                answer.setPointsEarned(0);
                userAnswerRepository.save(answer);
            }
        }
        
        deleteUserResponsesFromMongo(matchId);
    }
    
    private void deleteUserResponsesFromMongo(Long matchId) {
        try {
            List<UserResponse> responses = userResponseRepository.findByMatchId(matchId);
            for (UserResponse response : responses) {
                userResponseRepository.delete(response);
            }
        } catch (Exception e) {
            System.err.println("MongoDB delete quiz responses error (non-fatal): " + e.getMessage());
        }
    }
    
    @Transactional
    public void saveCorrectAnswersToMongo(Long matchId, Map<String, String> correctAnswers) {
        QuizCorrectAnswers existing = quizCorrectAnswersRepository.findByMatchId(matchId)
                .orElse(new QuizCorrectAnswers());
        
        existing.setMatchId(matchId);
        existing.setCorrectAnswers(correctAnswers);
        existing.setUpdatedAt(System.currentTimeMillis());
        
        quizCorrectAnswersRepository.save(existing);
    }
    
    public Map<String, String> getCorrectAnswersFromMongo(Long matchId) {
        return quizCorrectAnswersRepository.findByMatchId(matchId)
                .map(QuizCorrectAnswers::getCorrectAnswers)
                .orElse(null);
    }
    
    @Transactional
    public List<Question> saveQuizQuestions(Long matchId, List<QuestionDTO> questionDTOs) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found: " + matchId));
        
        List<Question> savedQuestions = new ArrayList<>();
        
        for (QuestionDTO dto : questionDTOs) {
            Question question;
            if (dto.getId() != null) {
                question = questionRepository.findById(dto.getId())
                        .orElse(new Question());
                if (question.getId() == null) {
                    question.setMatch(match);
                }
            } else {
                question = new Question();
                question.setMatch(match);
            }
            
            question.setQuestionText(dto.getQuestionText());
            question.setOptionA(dto.getOptionA());
            question.setOptionB(dto.getOptionB());
            question.setOptionC(dto.getOptionC());
            question.setOptionD(dto.getOptionD());
            question.setCorrectOption(dto.getCorrectOption() != null ? dto.getCorrectOption() : "");
            question.setPointsValue(dto.getPointsValue() != null ? dto.getPointsValue() : 10);
            question.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
            question.setQuestionType(dto.getQuestionType() != null ? dto.getQuestionType() : "QUIZ");
            question.setCreatedAt(System.currentTimeMillis());
            
            savedQuestions.add(questionRepository.save(question));
        }
        
        return savedQuestions;
    }
    
    private QuestionDTO convertToDTO(Question question) {
        QuestionDTO dto = new QuestionDTO();
        dto.setId(question.getId());
        dto.setMatchId(question.getMatch().getId());
        dto.setQuestionText(question.getQuestionText());
        dto.setOptionA(question.getOptionA());
        dto.setOptionB(question.getOptionB());
        dto.setOptionC(question.getOptionC());
        dto.setOptionD(question.getOptionD());
        dto.setCorrectOption(question.getCorrectOption());
        dto.setPointsValue(question.getPointsValue());
        dto.setIsActive(question.getIsActive());
        dto.setQuestionType(question.getQuestionType());
        dto.setCreatedAt(question.getCreatedAt());
        return dto;
    }
    
    private UserAnswerDTO convertAnswerToDTO(UserAnswer answer) {
        UserAnswerDTO dto = new UserAnswerDTO();
        dto.setId(answer.getId());
        dto.setUserId(answer.getUser().getId());
        dto.setUsername(answer.getUser().getUsername());
        dto.setQuestionId(answer.getQuestion().getId());
        dto.setSelectedOption(answer.getSelectedOption());
        dto.setIsCorrect(answer.getIsCorrect());
        dto.setPointsEarned(answer.getPointsEarned());
        dto.setAnsweredAt(answer.getAnsweredAt());
        return dto;
    }
    
    private Map<String, String> buildOptionMap(Question q) {
        Map<String, String> map = new HashMap<>();
        map.put("A", q.getOptionA());
        map.put("B", q.getOptionB());
        if (q.getOptionC() != null && !q.getOptionC().isEmpty()) map.put("C", q.getOptionC());
        if (q.getOptionD() != null && !q.getOptionD().isEmpty()) map.put("D", q.getOptionD());
        return map;
    }
    
    /**
     * Builds the default question option map for a match when no H2 questions are uploaded.
     * Q1 is the toss question with team names; Q2-5 are static.
     */
    private Map<Long, Map<String, String>> buildDefaultQuestionOptionsForMatch(Match match) {
        Map<Long, Map<String, String>> map = new HashMap<>();
        // Q1: Toss - use team short names or full names
        Map<String, String> q1 = new HashMap<>();
        String homeTeam = "Home Team";
        String awayTeam = "Away Team";
        if (match.getHomeTeam() != null) {
            homeTeam = match.getHomeTeam().getShortName() != null ? match.getHomeTeam().getShortName() : match.getHomeTeam().getTeamName();
        }
        if (match.getAwayTeam() != null) {
            awayTeam = match.getAwayTeam().getShortName() != null ? match.getAwayTeam().getShortName() : match.getAwayTeam().getTeamName();
        }
        q1.put("A", homeTeam);
        q1.put("B", awayTeam);
        map.put(1L, q1);
        // Q2
        Map<String, String> q2 = new HashMap<>();
        q2.put("A", "Under 30"); q2.put("B", "30-50"); q2.put("C", "50-70"); q2.put("D", "70+");
        map.put(2L, q2);
        // Q3
        Map<String, String> q3 = new HashMap<>();
        q3.put("A", "Under 10"); q3.put("B", "10-15"); q3.put("C", "15-20"); q3.put("D", "20+");
        map.put(3L, q3);
        // Q4
        Map<String, String> q4 = new HashMap<>();
        q4.put("A", "Under 6"); q4.put("B", "6-7"); q4.put("C", "7-8"); q4.put("D", "8+");
        map.put(4L, q4);
        // Q5
        Map<String, String> q5 = new HashMap<>();
        q5.put("A", "Yes"); q5.put("B", "No");
        map.put(5L, q5);
        return map;
    }
}
