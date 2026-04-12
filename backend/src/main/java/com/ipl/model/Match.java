package com.ipl.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Match {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_team_id")
    private Team winnerTeam;
    
    @Column(nullable = false)
    private String venue;
    
    @Column(nullable = false)
    private Long matchDate;
    
    @Column(nullable = false)
    private Integer matchNumber;
    
    private String matchStatus;
    
    @Column(nullable = false)
    private String matchType;
    
    private Integer homeTeamScore;
    
    private Integer awayTeamScore;
    
    private String homeTeamOvers;
    
    private String awayTeamOvers;
    
    private String result;
    
    private Integer homeWinProbability;
    
    private Integer awayWinProbability;
    
    private Integer matchDuration;
    
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    private List<Prediction> predictions;
}