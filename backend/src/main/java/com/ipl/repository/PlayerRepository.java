package com.ipl.repository;

import com.ipl.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    
    Optional<Player> findByPlayerName(String playerName);
    
    Optional<Player> findByShortName(String shortName);
    
    List<Player> findByTeamId(Long teamId);
    
    List<Player> findByRole(String role);
    
    boolean existsByPlayerName(String playerName);
    
    boolean existsByShortName(String shortName);
    
    @Query("SELECT p FROM Player p WHERE p.team.id = :teamId ORDER BY p.runs DESC")
    List<Player> findTopPlayersByTeam(Long teamId);
    
    @Query("SELECT p FROM Player p WHERE p.role = :role ORDER BY p.wickets DESC")
    List<Player> findTopBowlersByRole(String role);
}