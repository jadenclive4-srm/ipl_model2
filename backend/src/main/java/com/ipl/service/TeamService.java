package com.ipl.service;

import com.ipl.model.Team;
import com.ipl.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TeamService {
    
    private final TeamRepository teamRepository;
    
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }
    
    public Optional<Team> getTeamById(Long id) {
        return teamRepository.findById(id);
    }
    
    public Optional<Team> getTeamByName(String teamName) {
        return teamRepository.findByTeamNameOrShortName(teamName);
    }
    
    public Optional<Team> getTeamByShortName(String shortName) {
        return teamRepository.findByShortName(shortName);
    }
    
    @Transactional
    public Team createTeam(String teamName, String shortName, String homeCity, String stadium, String teamColor) {
        if (teamRepository.existsByTeamName(teamName)) {
            throw new RuntimeException("Team name already exists");
        }
        if (teamRepository.existsByShortName(shortName)) {
            throw new RuntimeException("Short name already exists");
        }
        
        Team team = new Team();
        team.setTeamName(teamName);
        team.setShortName(shortName);
        team.setHomeCity(homeCity);
        team.setStadium(stadium);
        team.setTeamColor(teamColor);
        team.setMatchesPlayed(0);
        team.setMatchesWon(0);
        team.setMatchesLost(0);
        team.setNetRunRate(0.0);
        team.setPoints(0);
        
        return teamRepository.save(team);
    }
    
    public List<Team> getTeamStandings() {
        return teamRepository.findAllByOrderByPointsDesc();
    }
    
    @Transactional
    public Team updateTeamStats(Long teamId, boolean isWin) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        
        team.setMatchesPlayed(team.getMatchesPlayed() + 1);
        
        if (isWin) {
            team.setMatchesWon(team.getMatchesWon() + 1);
            team.setPoints(team.getPoints() + 2);
        } else {
            team.setMatchesLost(team.getMatchesLost() + 1);
        }
        
        return teamRepository.save(team);
    }
    
    @Transactional
    public Team updateNetRunRate(Long teamId, double nrrChange) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        
        team.setNetRunRate(team.getNetRunRate() + nrrChange);
        return teamRepository.save(team);
    }
    
    @Transactional
    public void deleteTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        
        teamRepository.delete(team);
    }
}