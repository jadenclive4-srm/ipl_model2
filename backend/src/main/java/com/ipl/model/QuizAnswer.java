package com.ipl.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "quiz_answers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizAnswer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;
    
    @Column(name = "question_id", nullable = false)
    private String questionId;
    
    @Column(name = "selected_option", nullable = false)
    private String selectedOption;
    
    @Column(name = "is_correct")
    private Boolean isCorrect = false;
    
    @Column(name = "points_earned")
    private Integer pointsEarned = 0;
    
    @Column(name = "created_at")
    private Long createdAt;
}
