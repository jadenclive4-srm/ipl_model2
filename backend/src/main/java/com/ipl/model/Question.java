package com.ipl.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;
    
    @Column(nullable = false, length = 1000)
    private String questionText;
    
    @Column(name = "option_a", nullable = false)
    private String optionA;
    
    @Column(name = "option_b", nullable = false)
    private String optionB;
    
    @Column(name = "option_c")
    private String optionC;
    
    @Column(name = "option_d")
    private String optionD;
    
    @Column(name = "correct_option", nullable = false)
    private String correctOption;
    
    @Column(nullable = false)
    private Integer pointsValue = 10;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "question_type", nullable = false)
    private String questionType;
    
    @Column(name = "created_at")
    private Long createdAt;
}