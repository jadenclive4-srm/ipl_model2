package com.ipl.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String playerName;
    
    @Column(unique = true, nullable = false)
    private String shortName;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
    
    @Column(nullable = false)
    private String role;
    
    private String battingStyle;
    
    private String bowlingStyle;
    
    private Integer age;
    
    private String nationality;
    
    private String imageUrl;
    
    @Column(nullable = false)
    private Integer matchesPlayed = 0;
    
    @Column(nullable = false)
    private Integer runs = 0;
    
    @Column(nullable = false)
    private Integer wickets = 0;
    
    @Column(nullable = false)
    private Double strikeRate = 0.0;
    
    @Column(nullable = false)
    private Double economy = 0.0;
}