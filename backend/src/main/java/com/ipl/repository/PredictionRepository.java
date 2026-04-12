package com.ipl.repository;

import com.ipl.model.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    
    List<Prediction> findByUserId(Long userId);
    
    List<Prediction> findByMatchId(Long matchId);
    
    Optional<Prediction> findByUserIdAndMatchId(Long userId, Long matchId);
    
    boolean existsByUserIdAndMatchId(Long userId, Long matchId);
    
    @Query("SELECT p FROM Prediction p WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    List<Prediction> findUserPredictions(Long userId);
    
    @Query("SELECT p FROM Prediction p WHERE p.match.id = :matchId ORDER BY p.pointsEarned DESC")
    List<Prediction> findMatchPredictions(Long matchId);
    
    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.isCorrect = true AND p.user.id = :userId")
    Long countCorrectPredictions(Long userId);
    
    @Query("SELECT SUM(p.pointsEarned) FROM Prediction p WHERE p.user.id = :userId")
    Long sumPointsByUser(Long userId);
    
    @Modifying
    @Query(value = "DELETE FROM predictions WHERE match_id = :matchId", nativeQuery = true)
    void deleteAllByMatchIdNative(Long matchId);
}