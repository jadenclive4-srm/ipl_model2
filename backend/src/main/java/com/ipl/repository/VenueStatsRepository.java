package com.ipl.repository;

import com.ipl.model.VenueStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VenueStatsRepository extends JpaRepository<VenueStats, Long> {
    Optional<VenueStats> findByStadium(String stadium);
    
    @Query("SELECT v FROM VenueStats v WHERE :venue LIKE CONCAT('%', v.stadium, '%') OR :venue LIKE CONCAT('%', v.city, '%')")
    Optional<VenueStats> findByVenueContaining(String venue);
}