package com.ipl.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "teams")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Team {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String teamName;
    
    @Column(unique = true, nullable = false)
    private String shortName;
    
    private String homeCity;
    
    private String stadium;
    
    private String logoUrl;
    
    private String teamColor;
    
    @Column(nullable = false)
    private Integer matchesPlayed = 0;
    
    @Column(nullable = false)
    private Integer matchesWon = 0;
    
    @Column(nullable = false)
    private Integer matchesLost = 0;
    
    @Column(nullable = false)
    private Double netRunRate = 0.0;
    
    @Column(nullable = false)
    private Integer points = 0;
    
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
    private List<Player> players;
    
    @OneToMany(mappedBy = "homeTeam", cascade = CascadeType.ALL)
    private List<Match> homeMatches;
    
    @OneToMany(mappedBy = "awayTeam", cascade = CascadeType.ALL)
    private List<Match> awayMatches;
}