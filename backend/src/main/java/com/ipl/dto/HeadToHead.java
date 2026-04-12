package com.ipl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeadToHead {
    private int totalMatches;
    private int team1Wins;
    private int team2Wins;
    private int draws;
    private String team1Name;
    private String team2Name;
}