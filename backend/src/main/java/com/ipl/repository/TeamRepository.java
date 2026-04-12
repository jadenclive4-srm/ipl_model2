package com.ipl.repository;

import com.ipl.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    
    Optional<Team> findByTeamName(String teamName);
    
    Optional<Team> findByShortName(String shortName);
    
    boolean existsByTeamName(String teamName);
    
    boolean existsByShortName(String shortName);
    
    @Query("SELECT t FROM Team t ORDER BY t.points DESC")
    List<Team> findAllByOrderByPointsDesc();
    
    @Query("SELECT t FROM Team t ORDER BY t.netRunRate DESC")
    List<Team> findAllByOrderByNetRunRateDesc();
    
    @Query("SELECT t FROM Team t WHERE LOWER(t.teamName) = LOWER(:name) OR LOWER(t.shortName) = LOWER(:name)")
    Optional<Team> findByTeamNameOrShortName(String name);
}