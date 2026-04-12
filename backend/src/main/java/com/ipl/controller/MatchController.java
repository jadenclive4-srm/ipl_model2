package com.ipl.controller;

import com.ipl.dto.HeadToHead;
import com.ipl.dto.MatchDTO;
import com.ipl.dto.VenueStatsDTO;
import com.ipl.model.Match;
import com.ipl.model.Team;
import com.ipl.model.VenueStats;
import com.ipl.service.MatchService;
import com.ipl.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {
    
    private final MatchService matchService;
    private final TeamService teamService;
    
    @GetMapping
    public ResponseEntity<List<MatchDTO>> getAllMatches() {
        List<MatchDTO> matches = matchService.getAllMatches().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(matches);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<MatchDTO> getMatchById(@PathVariable Long id) {
        return matchService.getMatchById(id)
                .map(match -> ResponseEntity.ok(convertToDTO(match)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/today")
    public ResponseEntity<MatchDTO> getTodayMatch() {
        return matchService.getTodayMatch()
                .map(match -> ResponseEntity.ok(convertToDTO(match)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/upcoming")
    public ResponseEntity<List<MatchDTO>> getUpcomingMatches() {
        List<MatchDTO> matches = matchService.getUpcomingMatches().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(matches);
    }

    @PostMapping("/import")
    public ResponseEntity<String> importMatches() {
        try {
            matchService.importMatchesFromExcel("..\\data\\matches.csv");
            return ResponseEntity.ok("Matches imported successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to import matches: " + e.getMessage());
        }
    }
    
    @PostMapping("/import/excel")
    public ResponseEntity<String> importMatchesFromExcel(@RequestParam("file") MultipartFile file) {
        try {
            int count = matchService.importMatchesFromExcelFile(file.getInputStream());
            return ResponseEntity.ok(count + " matches imported successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to import matches: " + e.getMessage());
        }
    }
    
    @PostMapping("/import/h2h")
    public ResponseEntity<String> importH2hStats() {
        try {
            int count = matchService.importH2hStatsFromClasspath("/data/h2h_stats.csv");
            return ResponseEntity.ok(count + " h2h stats imported successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to import h2h stats: " + e.getMessage());
        }
    }
    
    @PostMapping("/import/venue")
    public ResponseEntity<String> importVenueStats() {
        try {
            int count = matchService.importVenueStatsFromClasspath("/data/venue.csv");
            return ResponseEntity.ok(count + " venue stats imported successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to import venue stats: " + e.getMessage());
        }
    }
    
    @GetMapping("/venue/{stadium}")
    public ResponseEntity<VenueStatsDTO> getVenueStats(@PathVariable String stadium) {
        VenueStats stats = matchService.getVenueStatsByStadium(stadium);
        if (stats == null) {
            return ResponseEntity.notFound().build();
        }
        VenueStatsDTO dto = new VenueStatsDTO();
        dto.setStadium(stats.getStadium());
        dto.setCity(stats.getCity());
        dto.setPitchType(stats.getPitchType());
        dto.setAvgScore(stats.getAvgScore());
        dto.setChasingWinPct(stats.getChasingWinPct());
        dto.setDewFactor(stats.getDewFactor());
        dto.setBoundarySize(stats.getBoundarySize());
        return ResponseEntity.ok(dto);
    }
    
    @GetMapping("/completed")
    public ResponseEntity<List<MatchDTO>> getCompletedMatches() {
        List<MatchDTO> matches = matchService.getCompletedMatches().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(matches);
    }
    
    @PostMapping
    public ResponseEntity<MatchDTO> createMatch(@RequestBody MatchDTO matchDTO) {
        Match match = matchService.createMatch(
                getTeamIdByName(matchDTO.getHomeTeamName()),
                getTeamIdByName(matchDTO.getAwayTeamName()),
                matchDTO.getVenue(),
                matchDTO.getMatchDate(),
                matchDTO.getMatchNumber(),
                matchDTO.getMatchType()
        );
        return ResponseEntity.ok(convertToDTO(match));
    }
    
    @PutMapping("/{id}/result")
    public ResponseEntity<MatchDTO> updateMatchResult(
            @PathVariable Long id,
            @RequestBody MatchDTO matchDTO) {
        Long winnerId = null;
        if (matchDTO.getWinnerTeamName() != null) {
            winnerId = getTeamIdByName(matchDTO.getWinnerTeamName());
        }
        
        Match match = matchService.updateMatchResult(
                id,
                winnerId,
                matchDTO.getHomeTeamScore(),
                matchDTO.getAwayTeamScore(),
                matchDTO.getResult()
        );
        return ResponseEntity.ok(convertToDTO(match));
    }
    
    @PostMapping("/{id}/reset")
    public ResponseEntity<MatchDTO> resetMatchResult(@PathVariable Long id) {
        Match match = matchService.resetMatchResult(id);
        return ResponseEntity.ok(convertToDTO(match));
    }
    
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<MatchDTO>> getMatchesByTeam(@PathVariable Long teamId) {
        List<MatchDTO> matches = matchService.getMatchesByTeam(teamId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(matches);
    }
    
    @GetMapping("/headtohead/{team1Id}/{team2Id}")
    public ResponseEntity<HeadToHead> getHeadToHeadStats(
            @PathVariable Long team1Id,
            @PathVariable Long team2Id) {
        HeadToHead stats = matchService.getHeadToHeadFromDb(team1Id, team2Id);
        if (stats == null || stats.getTotalMatches() == 0) {
            stats = matchService.getHeadToHeadStats(team1Id, team2Id);
        }
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/h2h")
    public ResponseEntity<HeadToHead> getH2hByTeamNames(
            @RequestParam String team1Name,
            @RequestParam String team2Name) {
        Long team1Id = getTeamIdByName(team1Name);
        Long team2Id = getTeamIdByName(team2Name);
        if (team1Id == null || team2Id == null) {
            return ResponseEntity.badRequest().build();
        }
        HeadToHead stats = matchService.getHeadToHeadFromDb(team1Id, team2Id);
        if (stats == null) {
            stats = matchService.getHeadToHeadStats(team1Id, team2Id);
        }
        return ResponseEntity.ok(stats);
    }
    
    private Long getTeamIdByName(String teamName) {
        return teamService.getTeamByName(teamName)
                .map(Team::getId)
                .orElse(null);
    }
    
    private MatchDTO convertToDTO(Match match) {
        MatchDTO dto = new MatchDTO();
        dto.setId(match.getId());
        dto.setHomeTeamName(match.getHomeTeam().getTeamName());
        dto.setAwayTeamName(match.getAwayTeam().getTeamName());
        dto.setHomeTeamShortName(match.getHomeTeam().getShortName());
        dto.setAwayTeamShortName(match.getAwayTeam().getShortName());
        dto.setVenue(match.getVenue());
        dto.setMatchDate(match.getMatchDate());
        dto.setMatchNumber(match.getMatchNumber());
        dto.setMatchStatus(match.getMatchStatus());
        dto.setMatchType(match.getMatchType());
        dto.setHomeTeamScore(match.getHomeTeamScore());
        dto.setAwayTeamScore(match.getAwayTeamScore());
        dto.setHomeTeamOvers(match.getHomeTeamOvers());
        dto.setAwayTeamOvers(match.getAwayTeamOvers());
        dto.setResult(match.getResult());
        
        if (match.getWinnerTeam() != null) {
            dto.setWinnerTeamName(match.getWinnerTeam().getTeamName());
        }
        
        dto.setHomeWinProbability(match.getHomeWinProbability());
        dto.setAwayWinProbability(match.getAwayWinProbability());
        dto.setMatchDuration(match.getMatchDuration());
        dto.setHomeTeamId(match.getHomeTeam().getId());
        dto.setAwayTeamId(match.getAwayTeam().getId());
        dto.setHomeTeamLogoUrl(match.getHomeTeam().getLogoUrl());
        dto.setAwayTeamLogoUrl(match.getAwayTeam().getLogoUrl());
        
        VenueStats venueStats = matchService.getVenueStatsByStadium(match.getVenue());
        if (venueStats != null) {
            VenueStatsDTO venueDTO = new VenueStatsDTO();
            venueDTO.setStadium(venueStats.getStadium());
            venueDTO.setCity(venueStats.getCity());
            venueDTO.setPitchType(venueStats.getPitchType());
            venueDTO.setAvgScore(venueStats.getAvgScore());
            venueDTO.setChasingWinPct(venueStats.getChasingWinPct());
            venueDTO.setDewFactor(venueStats.getDewFactor());
            venueDTO.setBoundarySize(venueStats.getBoundarySize());
            dto.setVenueStats(venueDTO);
        }
        
        return dto;
    }
}