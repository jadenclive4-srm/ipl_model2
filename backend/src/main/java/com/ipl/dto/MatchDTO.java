package com.ipl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchDTO {
    
    private Long id;
    private String homeTeamName;
    private String awayTeamName;
    private String homeTeamShortName;
    private String awayTeamShortName;
    private String venue;
    private Long matchDate;
    private Integer matchNumber;
    private String matchStatus;
    private String matchType;
    private Integer homeTeamScore;
    private Integer awayTeamScore;
    private String homeTeamOvers;
    private String awayTeamOvers;
    private String winnerTeamName;
    private String result;
    private Integer homeWinProbability;
    private Integer awayWinProbability;
    private Integer matchDuration;
    private Long homeTeamId;
    private Long awayTeamId;
    private String homeTeamLogoUrl;
    private String awayTeamLogoUrl;
    private VenueStatsDTO venueStats;
}