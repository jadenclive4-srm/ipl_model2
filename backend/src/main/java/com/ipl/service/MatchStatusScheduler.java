package com.ipl.service;

import com.ipl.model.Match;
import com.ipl.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MatchStatusScheduler {

    private final MatchRepository matchRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void updateMatchStatuses() {
        long now = System.currentTimeMillis();
        List<Match> allMatches = matchRepository.findAll();
        List<Match> updatedMatches = new ArrayList<>();

        for (Match match : allMatches) {
            long startTime = match.getMatchDate();
            int defaultDuration = 240;
            int duration = match.getMatchDuration() != null ? match.getMatchDuration() : defaultDuration;
            long estimatedEndTime = startTime + (long) duration * 60 * 1000;

            String currentStatus = match.getMatchStatus();
            
            if ("SCHEDULED".equals(currentStatus) && now >= startTime - 3600000) {
                match.setMatchStatus("UPCOMING");
                updatedMatches.add(match);
            } else if (("SCHEDULED".equals(currentStatus) || "UPCOMING".equals(currentStatus)) && now >= startTime) {
                match.setMatchStatus("LIVE");
                updatedMatches.add(match);
            } else if ("LIVE".equals(currentStatus) && match.getWinnerTeam() != null) {
                match.setMatchStatus("COMPLETED");
                updatedMatches.add(match);
            }
        }
        
        if (!updatedMatches.isEmpty()) {
            matchRepository.saveAll(updatedMatches);
        }
    }
}