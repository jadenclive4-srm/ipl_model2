package com.ipl.service;

import com.ipl.model.Player;
import com.ipl.model.Team;
import com.ipl.repository.PlayerRepository;
import com.ipl.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlayerService {
    
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    
    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }
    
    public Optional<Player> getPlayerById(Long id) {
        return playerRepository.findById(id);
    }
    
    public List<Player> getPlayersByTeam(Long teamId) {
        return playerRepository.findByTeamId(teamId);
    }
    
    public List<Player> getPlayersByRole(String role) {
        return playerRepository.findByRole(role);
    }
    
    @Transactional
    public Player createPlayer(String playerName, String shortName, Long teamId, String role, 
                          String battingStyle, String bowlingStyle, Integer age, String nationality) {
        if (playerRepository.existsByPlayerName(playerName)) {
            throw new RuntimeException("Player name already exists");
        }
        if (playerRepository.existsByShortName(shortName)) {
            throw new RuntimeException("Short name already exists");
        }
        
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        
        Player player = new Player();
        player.setPlayerName(playerName);
        player.setShortName(shortName);
        player.setTeam(team);
        player.setRole(role);
        player.setBattingStyle(battingStyle);
        player.setBowlingStyle(bowlingStyle);
        player.setAge(age);
        player.setNationality(nationality);
        player.setMatchesPlayed(0);
        player.setRuns(0);
        player.setWickets(0);
        player.setStrikeRate(0.0);
        player.setEconomy(0.0);
        
        return playerRepository.save(player);
    }
    
    public List<Player> getTopPlayersByTeam(Long teamId) {
        return playerRepository.findTopPlayersByTeam(teamId);
    }
    
    @Transactional
    public Player updatePlayerStats(Long playerId, int runs, int wickets, double strikeRate, double economy) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));
        
        player.setRuns(player.getRuns() + runs);
        player.setWickets(player.getWickets() + wickets);
        player.setStrikeRate(strikeRate);
        player.setEconomy(economy);
        player.setMatchesPlayed(player.getMatchesPlayed() + 1);
        
        return playerRepository.save(player);
    }
    
    @Transactional
    public void deletePlayer(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));
        
        playerRepository.delete(player);
    }
}