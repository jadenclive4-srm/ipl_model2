package com.ipl.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "venue_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VenueStats {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "stadium", nullable = false)
    private String stadium;
    
    @Column(name = "city")
    private String city;
    
    @Column(name = "pitch_type")
    private String pitchType;
    
    @Column(name = "avg_score")
    private Integer avgScore;
    
    @Column(name = "chasing_win_pct")
    private Integer chasingWinPct;
    
    @Column(name = "dew_factor")
    private String dewFactor;
    
    @Column(name = "boundary_size")
    private String boundarySize;
}