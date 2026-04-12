package com.ipl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamDTO {
    
    private Long id;
    private String teamName;
    private String shortName;
    private String homeCity;
    private String stadium;
    private String logoUrl;
    private String teamColor;
    private Integer matchesPlayed;
    private Integer matchesWon;
    private Integer matchesLost;
    private Double netRunRate;
    private Integer points;
}