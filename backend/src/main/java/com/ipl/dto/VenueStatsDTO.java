package com.ipl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VenueStatsDTO {
    private String stadium;
    private String city;
    private String pitchType;
    private Integer avgScore;
    private Integer chasingWinPct;
    private String dewFactor;
    private String boundarySize;
}