package com.ipl.repository;

import com.ipl.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    @Query("SELECT m FROM Match m ORDER BY m.matchNumber ASC")
    List<Match> findAllOrderedByMatchNumber();

    @Query("SELECT m FROM Match m WHERE m.matchDate >= :currentTime ORDER BY m.matchDate ASC")
    List<Match> findUpcomingMatches(Long currentTime);
    
    @Query("SELECT m FROM Match m WHERE m.matchDate < :currentTime ORDER BY m.matchDate DESC")
    List<Match> findCompletedMatches(Long currentTime);
    
    @Query("SELECT m FROM Match m WHERE m.matchDate >= :startTime AND m.matchDate <= :endTime ORDER BY m.matchDate ASC")
    List<Match> findMatchesByDateRange(Long startTime, Long endTime);
    
    @Query("SELECT m FROM Match m WHERE m.homeTeam.id = :teamId OR m.awayTeam.id = :teamId ORDER BY m.matchDate DESC")
    List<Match> findMatchesByTeam(Long teamId);
    
    Optional<Match> findByMatchNumber(Integer matchNumber);
    
    @Query("SELECT m FROM Match m WHERE m.matchDate >= :currentTime ORDER BY m.matchDate ASC LIMIT 1")
    Optional<Match> findNextMatch(Long currentTime);
    
    @Query("SELECT m FROM Match m WHERE m.matchDate >= :startOfDay AND m.matchDate < :endOfDay ORDER BY m.matchDate ASC")
    List<Match> findMatchesForToday(Long startOfDay, Long endOfDay);
    
    @Query("SELECT COUNT(m) FROM Match m WHERE m.matchStatus = :status")
    Long countByMatchStatus(String status);
    
    @Query("SELECT m FROM Match m WHERE m.matchStatus = 'COMPLETED' AND ((m.homeTeam.id = :team1Id AND m.awayTeam.id = :team2Id) OR (m.homeTeam.id = :team2Id AND m.awayTeam.id = :team1Id)) ORDER BY m.matchDate DESC")
    List<Match> findHeadToHeadMatches(Long team1Id, Long team2Id);
    
    @Query("SELECT m FROM Match m WHERE m.matchStatus = :status ORDER BY m.matchDate DESC")
    List<Match> findByMatchStatus(String status);
}