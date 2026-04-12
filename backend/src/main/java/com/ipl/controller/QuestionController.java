package com.ipl.controller;

import com.ipl.dto.QuestionDTO;
import com.ipl.dto.UserAnswerDTO;
import com.ipl.model.Question;
import com.ipl.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {
    
    private final QuestionService questionService;
    
    @GetMapping("/match/{matchId}")
    public ResponseEntity<List<QuestionDTO>> getQuestionsByMatchId(
            @PathVariable Long matchId,
            @RequestParam Long userId) {
        List<QuestionDTO> questions = questionService.getQuestionsByMatchId(matchId, userId);
        return ResponseEntity.ok(questions);
    }
    
    @PostMapping("/answer")
    public ResponseEntity<UserAnswerDTO> submitAnswer(
            @RequestParam Long userId,
            @RequestParam Long questionId,
            @RequestParam String selectedOption) {
        UserAnswerDTO answer = questionService.submitAnswer(userId, questionId, selectedOption);
        return ResponseEntity.ok(answer);
    }
    
    @PostMapping("/answers/batch")
    public ResponseEntity<List<UserAnswerDTO>> submitAnswers(@RequestBody BatchAnswerRequest request) {
        List<UserAnswerDTO> result = questionService.submitAnswers(request.getUserId(), request.getMatchId(), request.getAnswers(), request.getQuestionIds());
        return ResponseEntity.ok(result);
    }
    
    // Inner class for batch answer request
    public static class BatchAnswerRequest {
        private Long userId;
        private Long matchId;
        private List<String> answers;
        private List<Long> questionIds;
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getMatchId() { return matchId; }
        public void setMatchId(Long matchId) { this.matchId = matchId; }
        public List<String> getAnswers() { return answers; }
        public void setAnswers(List<String> answers) { this.answers = answers; }
        public List<Long> getQuestionIds() { return questionIds; }
        public void setQuestionIds(List<Long> questionIds) { this.questionIds = questionIds; }
    }
    
    @GetMapping("/user/match/{matchId}")
    public ResponseEntity<List<UserAnswerDTO>> getUserAnswers(
            @RequestParam Long userId,
            @PathVariable Long matchId) {
        List<UserAnswerDTO> answers = questionService.getUserAnswersByMatchId(userId, matchId);
        return ResponseEntity.ok(answers);
    }
    
    @GetMapping("/match/{matchId}/all")
    public ResponseEntity<List<QuestionDTO>> getAllQuestionsByMatchId(@PathVariable Long matchId) {
        List<Question> questions = questionService.getAllQuestionsByMatchId(matchId);
        List<QuestionDTO> dtos = new java.util.ArrayList<>();
        for (Question q : questions) {
            QuestionDTO dto = new QuestionDTO();
            dto.setId(q.getId());
            dto.setMatchId(q.getMatch().getId());
            dto.setQuestionText(q.getQuestionText());
            dto.setOptionA(q.getOptionA());
            dto.setOptionB(q.getOptionB());
            dto.setOptionC(q.getOptionC());
            dto.setOptionD(q.getOptionD());
            dto.setCorrectOption(q.getCorrectOption());
            dto.setPointsValue(q.getPointsValue());
            dto.setIsActive(q.getIsActive());
            dto.setQuestionType(q.getQuestionType());
            dto.setCreatedAt(q.getCreatedAt());
            dtos.add(dto);
        }
        return ResponseEntity.ok(dtos);
    }
    
    @PutMapping("/{questionId}/correct")
    public ResponseEntity<QuestionDTO> updateQuestionCorrectOption(
            @PathVariable Long questionId,
            @RequestBody QuestionDTO dto) {
        Question question = questionService.updateQuestionCorrectOption(questionId, dto.getCorrectOption());
        QuestionDTO result = new QuestionDTO();
        result.setId(question.getId());
        result.setCorrectOption(question.getCorrectOption());
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/evaluate/match/{matchId}")
    public ResponseEntity<Map<String, String>> evaluateQuizAnswers(@PathVariable Long matchId) {
        questionService.evaluateQuizAnswers(matchId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Quiz answers evaluated successfully");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/reset/match/{matchId}")
    public ResponseEntity<Map<String, String>> resetQuizAnswers(@PathVariable Long matchId) {
        questionService.resetQuizAnswers(matchId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Quiz answers reset successfully");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/match/{matchId}/correct-answers")
    public ResponseEntity<String> saveCorrectAnswers(
            @PathVariable Long matchId,
            @RequestBody Map<String, String> correctAnswers) {
        questionService.saveCorrectAnswersToMongo(matchId, correctAnswers);
        return ResponseEntity.ok("Correct answers saved successfully");
    }
    
    @GetMapping("/match/{matchId}/correct-answers")
    public ResponseEntity<Map<String, String>> getCorrectAnswers(@PathVariable Long matchId) {
        Map<String, String> correctAnswers = questionService.getCorrectAnswersFromMongo(matchId);
        return ResponseEntity.ok(correctAnswers);
    }
    
    @PostMapping("/evaluate-from-db/match/{matchId}")
    public ResponseEntity<String> evaluateFromMongo(@PathVariable Long matchId) {
        questionService.evaluateQuizAnswersFromMongo(matchId);
        return ResponseEntity.ok("Quiz evaluated from database successfully");
    }
    
    @PostMapping("/match/{matchId}/upload")
    public ResponseEntity<List<QuestionDTO>> uploadQuizQuestions(
            @PathVariable Long matchId,
            @RequestBody List<QuestionDTO> questions) {
        List<Question> saved = questionService.saveQuizQuestions(matchId, questions);
        List<QuestionDTO> result = new ArrayList<>();
        for (Question q : saved) {
            QuestionDTO dto = new QuestionDTO();
            dto.setId(q.getId());
            dto.setMatchId(q.getMatch().getId());
            dto.setQuestionText(q.getQuestionText());
            dto.setOptionA(q.getOptionA());
            dto.setOptionB(q.getOptionB());
            dto.setOptionC(q.getOptionC());
            dto.setOptionD(q.getOptionD());
            dto.setCorrectOption(q.getCorrectOption());
            dto.setPointsValue(q.getPointsValue());
            dto.setIsActive(q.getIsActive());
            dto.setQuestionType(q.getQuestionType());
            result.add(dto);
        }
        return ResponseEntity.ok(result);
    }
}