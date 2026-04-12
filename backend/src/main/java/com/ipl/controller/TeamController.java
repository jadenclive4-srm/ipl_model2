package com.ipl.controller;

import com.ipl.dto.TeamDTO;
import com.ipl.model.Team;
import com.ipl.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {
    
    private final TeamService teamService;
    
    @GetMapping
    public ResponseEntity<List<TeamDTO>> getAllTeams() {
        List<TeamDTO> teams = teamService.getAllTeams().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(teams);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TeamDTO> getTeamById(@PathVariable Long id) {
        return teamService.getTeamById(id)
                .map(team -> ResponseEntity.ok(convertToDTO(team)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/name/{teamName}")
    public ResponseEntity<TeamDTO> getTeamByName(@PathVariable String teamName) {
        return teamService.getTeamByName(teamName)
                .map(team -> ResponseEntity.ok(convertToDTO(team)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<TeamDTO> createTeam(@RequestBody TeamDTO teamDTO) {
        Team team = teamService.createTeam(
                teamDTO.getTeamName(),
                teamDTO.getShortName(),
                teamDTO.getHomeCity(),
                teamDTO.getStadium(),
                teamDTO.getTeamColor()
        );
        return ResponseEntity.ok(convertToDTO(team));
    }
    
    @GetMapping("/standings")
    public ResponseEntity<List<TeamDTO>> getTeamStandings() {
        List<TeamDTO> teams = teamService.getTeamStandings().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(teams);
    }
    
    private TeamDTO convertToDTO(Team team) {
        TeamDTO dto = new TeamDTO();
        dto.setId(team.getId());
        dto.setTeamName(team.getTeamName());
        dto.setShortName(team.getShortName());
        dto.setHomeCity(team.getHomeCity());
        dto.setStadium(team.getStadium());
        dto.setLogoUrl(team.getLogoUrl());
        dto.setTeamColor(team.getTeamColor());
        dto.setMatchesPlayed(team.getMatchesPlayed());
        dto.setMatchesWon(team.getMatchesWon());
        dto.setMatchesLost(team.getMatchesLost());
        dto.setNetRunRate(team.getNetRunRate());
        dto.setPoints(team.getPoints());
        return dto;
    }
}