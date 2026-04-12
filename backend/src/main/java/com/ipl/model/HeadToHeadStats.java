package com.ipl.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "h2h_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeadToHeadStats {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "team1_id")
    private Team team1;
    
    @ManyToOne
    @JoinColumn(name = "team2_id")
    private Team team2;
    
    @Column(name = "total_matches")
    private Integer totalMatches;
    @Column(name = "team1_wins")
    private Integer team1Wins;
    
    @Column(name = "team2_wins")
    private Integer team2Wins;
}