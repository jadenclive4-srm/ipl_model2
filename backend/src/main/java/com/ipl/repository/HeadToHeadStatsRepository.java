package com.ipl.repository;

import com.ipl.model.HeadToHeadStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface HeadToHeadStatsRepository extends JpaRepository<HeadToHeadStats, Long> {
    
    @Query("SELECT h FROM HeadToHeadStats h WHERE (h.team1.id = :team1Id AND h.team2.id = :team2Id) OR (h.team1.id = :team2Id AND h.team2.id = :team1Id)")
    Optional<HeadToHeadStats> findByTeamIds(@Param("team1Id") Long team1Id, @Param("team2Id") Long team2Id);
}