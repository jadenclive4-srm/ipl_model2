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
import com.ipl.repository.mongo.UserResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuestionService {
    
    private final QuestionRepository questionRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final UserResponseRepository userResponseRepository;
    private final QuizCorrectAnswersRepository quizCorrectAnswersRepository;
    private final UserPointsService userPointsService;
    
    public List<QuestionDTO> getQuestionsByMatchId(Long matchId, Long userId) {
        List<Question> questions = questionRepository.findActiveQuestionsByMatchId(matchId);
        List<QuestionDTO> questionDTOs = new ArrayList<>();
        
        for (Question question : questions) {
            QuestionDTO dto = convertToDTO(question);
            
            // Check if user has already answered this question
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        
        // Check if user already answered this question
        Optional<UserAnswer> existingAnswer = userAnswerRepository.findByUserIdAndQuestionId(userId, questionId);
        
        UserAnswer userAnswer;
        if (existingAnswer.isPresent()) {
            // Update existing answer
            userAnswer = existingAnswer.get();
            userAnswer.setSelectedOption(selectedOption);
        } else {
            // Create new answer
            userAnswer = new UserAnswer();
            userAnswer.setUser(user);
            userAnswer.setQuestion(question);
            userAnswer.setSelectedOption(selectedOption);
            userAnswer.setAnsweredAt(System.currentTimeMillis());
        }
        
        // Check if answer is correct
        boolean isCorrect = selectedOption.equalsIgnoreCase(question.getCorrectOption());
        userAnswer.setIsCorrect(isCorrect);
        
        if (isCorrect) {
            userAnswer.setPointsEarned(PointsConfig.QUIZ_CORRECT);
        } else {
            userAnswer.setPointsEarned(0);
        }
        
        userAnswer = userAnswerRepository.save(userAnswer);
        
        if (isCorrect) {
            userRepository.incrementPoints(userId, PointsConfig.QUIZ_CORRECT);
        }
        
        return convertAnswerToDTO(userAnswer);
    }
    
    @Transactional
    public List<UserAnswerDTO> submitAnswers(Long userId, Long matchId, List<String> answers, List<Long> questionIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<UserAnswerDTO> result = new ArrayList<>();
        List<UserResponse.QuestionResponse> questionResponses = new ArrayList<>();
        
        for (int i = 0; i < questionIds.size(); i++) {
            UserAnswerDTO answer = submitAnswer(userId, questionIds.get(i), answers.get(i));
            result.add(answer);
            
            UserResponse.QuestionResponse qr = new UserResponse.QuestionResponse();
            qr.setQuestionId(questionIds.get(i));
            qr.setSelectedOption(answers.get(i));
            qr.setIsCorrect(answer.getIsCorrect());
            qr.setPointsEarned(answer.getPointsEarned());
            questionResponses.add(qr);
        }
        
        saveUserResponseToMongo(userId, user.getUsername(), matchId, questionResponses);
        
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
        List<Question> questions = questionRepository.findAllByMatchId(matchId);
        
        for (Question question : questions) {
            List<UserAnswer> answers = userAnswerRepository.findByQuestionId(question.getId());
            
            for (UserAnswer answer : answers) {
                boolean isCorrect = answer.getSelectedOption().equalsIgnoreCase(question.getCorrectOption());
                answer.setIsCorrect(isCorrect);
                
                if (isCorrect) {
                    answer.setPointsEarned(PointsConfig.QUIZ_CORRECT);
                    userRepository.incrementPoints(answer.getUser().getId(), PointsConfig.QUIZ_CORRECT);
                    userPointsService.updateUserPoints(answer.getUser().getId(), PointsConfig.QUIZ_CORRECT);
                } else {
                    answer.setPointsEarned(0);
                }
                
                userAnswerRepository.save(answer);
            }
        }
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
                        userRepository.incrementPoints(userId, -answer.getPointsEarned());
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
    public void evaluateQuizAnswersFromMongo(Long matchId) {
        Map<String, String> correctAnswers = getCorrectAnswersFromMongo(matchId);
        
        if (correctAnswers == null || correctAnswers.isEmpty()) {
            throw new RuntimeException("No correct answers found in database. Please save correct answers first.");
        }
        
        List<Question> questions = questionRepository.findAllByMatchId(matchId);
        
        for (Question question : questions) {
            List<UserAnswer> answers = userAnswerRepository.findByQuestionId(question.getId());
            String correctOption = correctAnswers.get(question.getId().toString());
            
            for (UserAnswer answer : answers) {
                boolean isCorrect = correctOption != null && 
                    answer.getSelectedOption().equalsIgnoreCase(correctOption);
                answer.setIsCorrect(isCorrect);
                
                if (isCorrect) {
                    answer.setPointsEarned(PointsConfig.QUIZ_CORRECT);
                    userRepository.incrementPoints(answer.getUser().getId(), PointsConfig.QUIZ_CORRECT);
                } else {
                    answer.setPointsEarned(0);
                }
                
                userAnswerRepository.save(answer);
            }
        }
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
}