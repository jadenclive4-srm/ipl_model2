package com.ipl.service;

import com.ipl.dto.HeadToHead;
import com.ipl.model.Match;
import com.ipl.model.HeadToHeadStats;
import com.ipl.model.Team;
import com.ipl.repository.MatchRepository;
import com.ipl.repository.TeamRepository;
import com.ipl.repository.HeadToHeadStatsRepository;
import com.ipl.repository.VenueStatsRepository;
import com.ipl.model.VenueStats;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MatchService {
    
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final HeadToHeadStatsRepository h2hStatsRepository;
    private final VenueStatsRepository venueStatsRepository;
    
    public List<Match> getAllMatches() {
        return matchRepository.findAllOrderedByMatchNumber();
    }
    
    public List<Match> getUpcomingMatches() {
        Long currentTime = System.currentTimeMillis();
        return matchRepository.findUpcomingMatches(currentTime);
    }
    
    public List<Match> getCompletedMatches() {
        Long currentTime = System.currentTimeMillis();
        return matchRepository.findCompletedMatches(currentTime);
    }
    
    public Optional<Match> getMatchById(Long id) {
        return matchRepository.findById(id);
    }
    
    public Optional<Match> getTodayMatch() {
        long now = System.currentTimeMillis();
        long startOfToday = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        List<Match> liveMatches = matchRepository.findByMatchStatus("LIVE");
        for (Match m : liveMatches) {
            if (m.getMatchDate() >= startOfToday) {
                return Optional.of(m);
            }
        }
        
        List<Match> upcomingMatches = matchRepository.findUpcomingMatches(now);
        if (!upcomingMatches.isEmpty()) {
            return Optional.of(upcomingMatches.get(0));
        }
        
        List<Match> scheduledMatches = matchRepository.findByMatchStatus("SCHEDULED");
        for (Match m : scheduledMatches) {
            if (m.getMatchDate() >= startOfToday) {
                return Optional.of(m);
            }
        }
        
        return Optional.empty();
    }
    
    @Transactional
    public Match createMatch(Long homeTeamId, Long awayTeamId, String venue, Long matchDate, Integer matchNumber, String matchType) {
        Team homeTeam = teamRepository.findById(homeTeamId)
                .orElseThrow(() -> new RuntimeException("Home team not found"));
        Team awayTeam = teamRepository.findById(awayTeamId)
                .orElseThrow(() -> new RuntimeException("Away team not found"));
        
        // Check if match with this number already exists
        Match match = matchRepository.findByMatchNumber(matchNumber)
                .orElse(new Match());
        
        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);
        match.setVenue(venue);
        match.setMatchDate(matchDate);
        match.setMatchNumber(matchNumber);
        match.setMatchType(matchType);
        match.setMatchStatus("SCHEDULED");
        match.setMatchDuration(240);
        
        return matchRepository.save(match);
    }
    
    @Transactional
    public Match updateMatchResult(Long matchId, Long winnerId, Integer homeScore, Integer awayScore, String result) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        
        if (winnerId != null) {
            Team winner = teamRepository.findById(winnerId)
                    .orElseThrow(() -> new RuntimeException("Winner team not found"));
            match.setWinnerTeam(winner);
        }
        
        match.setHomeTeamScore(homeScore);
        match.setAwayTeamScore(awayScore);
        match.setResult(result);
        match.setMatchStatus("COMPLETED");
        
        return matchRepository.save(match);
    }
    
    @Transactional
    public Match updateMatchResultByTeamName(Long matchId, String winnerTeamName, Integer homeScore, Integer awayScore, String result) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        
        if (winnerTeamName != null && !winnerTeamName.isEmpty()) {
            Team winner = teamRepository.findByTeamNameOrShortName(winnerTeamName)
                    .orElseThrow(() -> new RuntimeException("Winner team not found: " + winnerTeamName));
            match.setWinnerTeam(winner);
        }
        
        match.setHomeTeamScore(homeScore);
        match.setAwayTeamScore(awayScore);
        match.setResult(result);
        match.setMatchStatus("COMPLETED");
        
        return matchRepository.save(match);
    }
    
    @Transactional
    public Match resetMatchResult(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        
        match.setWinnerTeam(null);
        match.setHomeTeamScore(null);
        match.setAwayTeamScore(null);
        match.setResult(null);
        match.setMatchStatus("SCHEDULED");
        
        return matchRepository.save(match);
    }
    
    public List<Match> getMatchesByTeam(Long teamId) {
        return matchRepository.findMatchesByTeam(teamId);
    }
    
    @Transactional
    public Match updateWinProbability(Long matchId, Integer homeProbability, Integer awayProbability) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        
        match.setHomeWinProbability(homeProbability);
        match.setAwayWinProbability(awayProbability);
        
        return matchRepository.save(match);
    }
    
    public List<Match> getMatchesByDateRange(Long startTime, Long endTime) {
        return matchRepository.findMatchesByDateRange(startTime, endTime);
    }
    
public HeadToHead getHeadToHeadStats(Long team1Id, Long team2Id) {
        List<Match> matches = matchRepository.findHeadToHeadMatches(team1Id, team2Id);
        
        int totalMatches = matches.size();
        int team1Wins = 0;
        int team2Wins = 0;
        
        for (Match match : matches) {
            if (match.getWinnerTeam() != null) {
                if (match.getWinnerTeam().getId().equals(team1Id)) {
                    team1Wins++;
                } else if (match.getWinnerTeam().getId().equals(team2Id)) {
                    team2Wins++;
                }
            }
        }
        
        String team1Name = teamRepository.findById(team1Id).map(Team::getTeamName).orElse("Team1");
        String team2Name = teamRepository.findById(team2Id).map(Team::getTeamName).orElse("Team2");
        
        return new HeadToHead(totalMatches, team1Wins, team2Wins, 0, team1Name, team2Name);
    }
 
@Transactional
    public void importMatchesFromExcel(InputStream inputStream) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            List<String[]> rows = reader.readAll();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M/d/yyyy H:mm");

            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length < 6) continue;

                int matchNumber = Integer.parseInt(row[0]);
                String dateStr = row[1];
                String timeStr = row[2];
                String homeTeamName = row[3];
                String awayTeamName = row[4];
                String venue = row[5];
                String matchType = "LEAGUE";

                LocalDateTime dateTime = LocalDateTime.parse(dateStr + " " + timeStr, dateFormatter);
                long matchDate = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

                Long homeTeamId = getTeamIdByName(homeTeamName);
                Long awayTeamId = getTeamIdByName(awayTeamName);

                createMatch(homeTeamId, awayTeamId, venue, matchDate, matchNumber, matchType);
            }
        }
    }
    
    @Transactional
    public void importMatchesFromExcel(String filePath) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> rows = reader.readAll();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M/d/yyyy H:mm");

            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length < 6) continue;

                int matchNumber = Integer.parseInt(row[0]);
                String dateStr = row[1];
                String timeStr = row[2];
                String homeTeamName = row[3];
                String awayTeamName = row[4];
                String venue = row[5];
                String matchType = "LEAGUE";

                LocalDateTime dateTime = LocalDateTime.parse(dateStr + " " + timeStr, dateFormatter);
                long matchDate = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

                Long homeTeamId = getTeamIdByName(homeTeamName);
                Long awayTeamId = getTeamIdByName(awayTeamName);

createMatch(homeTeamId, awayTeamId, venue, matchDate, matchNumber, matchType);
            }
        }
    }
    
    @Transactional
    public int importMatchesFromExcelFile(InputStream inputStream) throws IOException {
        int importedCount = 0;
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                XSSFRow row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    int matchNumber = (int) row.getCell(0).getNumericCellValue();
                    String dateStr = row.getCell(1).getStringCellValue();
                    String timeStr = row.getCell(2).getStringCellValue();
                    String homeTeamName = row.getCell(3).getStringCellValue();
                    String awayTeamName = row.getCell(4).getStringCellValue();
                    String venue = row.getCell(5).getStringCellValue();
                    
                    String matchType = "LEAGUE";
                    if (row.getCell(6) != null && row.getCell(6).getCellType() != CellType.BLANK) {
                        matchType = row.getCell(6).getStringCellValue();
                    }
                    
                    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M/d/yyyy H:mm");
                    LocalDateTime dateTime = LocalDateTime.parse(dateStr + " " + timeStr, dateFormatter);
                    long matchDate = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    
                    Long homeTeamId = getTeamIdByName(homeTeamName);
                    Long awayTeamId = getTeamIdByName(awayTeamName);
                    
                    Match match = matchRepository.findByMatchNumber(matchNumber).orElse(new Match());
                    match.setHomeTeam(teamRepository.findById(homeTeamId).orElse(null));
                    match.setAwayTeam(teamRepository.findById(awayTeamId).orElse(null));
                    match.setVenue(venue);
                    match.setMatchDate(matchDate);
                    match.setMatchNumber(matchNumber);
                    match.setMatchType(matchType);
                    
                    if (row.getCell(7) != null && row.getCell(7).getCellType() != CellType.BLANK) {
                        String winnerTeamName = row.getCell(7).getStringCellValue();
                        Long winnerId = getTeamIdByName(winnerTeamName);
                        match.setWinnerTeam(teamRepository.findById(winnerId).orElse(null));
                    }
                    
                    if (row.getCell(8) != null && row.getCell(8).getCellType() != CellType.BLANK) {
                        match.setHomeTeamScore((int) row.getCell(8).getNumericCellValue());
                    }
                    if (row.getCell(9) != null && row.getCell(9).getCellType() != CellType.BLANK) {
                        match.setAwayTeamScore((int) row.getCell(9).getNumericCellValue());
                    }
                    if (row.getCell(10) != null && row.getCell(10).getCellType() != CellType.BLANK) {
                        match.setResult(row.getCell(10).getStringCellValue());
                    }
                    
                    if (match.getWinnerTeam() != null) {
                        match.setMatchStatus("COMPLETED");
                    } else {
                        match.setMatchStatus("SCHEDULED");
                    }
                    
                    matchRepository.save(match);
                    importedCount++;
                } catch (Exception e) {
                    System.err.println("Error importing row " + i + ": " + e.getMessage());
                }
            }
        }
        return importedCount;
    }
    
    @Transactional
    public int importH2hStatsFromCsv(String filePath) throws IOException, CsvException {
        int importedCount = 0;
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> rows = reader.readAll();
            
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length < 5) continue;
                
                try {
                    String team1Name = row[0].trim();
                    String team2Name = row[1].trim();
                    int totalMatches = Integer.parseInt(row[2].trim());
                    int team1Wins = Integer.parseInt(row[3].trim());
                    int team2Wins = Integer.parseInt(row[4].trim());
                    
                    Team team1 = teamRepository.findByShortName(team1Name)
                            .orElseThrow(() -> new RuntimeException("Team not found: " + team1Name));
                    Team team2 = teamRepository.findByShortName(team2Name)
                            .orElseThrow(() -> new RuntimeException("Team not found: " + team2Name));
                    
                    HeadToHeadStats stats = h2hStatsRepository.findByTeamIds(team1.getId(), team2.getId())
                            .orElse(new HeadToHeadStats());
                    
                    stats.setTeam1(team1);
                    stats.setTeam2(team2);
                    stats.setTotalMatches(totalMatches);
                    stats.setTeam1Wins(team1Wins);
                    stats.setTeam2Wins(team2Wins);
                    
                    h2hStatsRepository.save(stats);
                    importedCount++;
                } catch (Exception e) {
                    System.err.println("Error importing h2h row " + i + ": " + e.getMessage());
                }
            }
        }
        return importedCount;
    }
    
    public int importH2hStatsFromClasspath(String classpath) throws IOException {
        int importedCount = 0;
        
        h2hStatsRepository.deleteAll();
        System.out.println("Cleared existing H2H stats");
        
        InputStream is = getClass().getResourceAsStream(classpath);
        if (is == null) {
            throw new IOException("Resource not found: " + classpath);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return 0;
            
            String[] teamColumns = headerLine.split(",");
            List<String> teamNames = new ArrayList<>();
            for (int i = 1; i < teamColumns.length; i++) {
                teamNames.add(teamColumns[i].trim());
            }
            
            Map<String, Team> teamMap = new HashMap<>();
            for (String shortName : teamNames) {
                Team team = teamRepository.findByShortName(shortName).orElse(null);
                if (team != null) {
                    teamMap.put(shortName, team);
                } else {
                    System.err.println("Team not found for shortName: " + shortName);
                }
            }
            
            Map<String, Map<String, Integer>> winsMap = new HashMap<>();
            
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 2) continue;
                
                String team1Name = parts[0].trim();
                Map<String, Integer> teamWins = new HashMap<>();
                
                for (int i = 1; i < parts.length && i <= teamNames.size(); i++) {
                    String team2Name = teamNames.get(i - 1);
                    String value = parts[i].trim();
                    if (!value.isEmpty() && !value.equals("-")) {
                        teamWins.put(team2Name, Integer.parseInt(value));
                    }
                }
                winsMap.put(team1Name, teamWins);
            }
            
            for (String team1Name : winsMap.keySet()) {
                Team team1 = teamMap.get(team1Name);
                if (team1 == null) {
                    System.err.println("H2H: Team1 not found: " + team1Name);
                    continue;
                }
                
                Map<String, Integer> teamWins = winsMap.get(team1Name);
                
                for (String team2Name : teamWins.keySet()) {
                    if (team1Name.equals(team2Name)) continue;
                    
                    Team team2 = teamMap.get(team2Name);
                    if (team2 == null) {
                        System.err.println("H2H: Team2 not found: " + team2Name);
                        continue;
                    }
                    
                    int team1WinsVal = teamWins.get(team2Name);
                    
                    Map<String, Integer> opponentWins = winsMap.get(team2Name);
                    int team2WinsVal = (opponentWins != null && opponentWins.get(team1Name) != null) 
                        ? opponentWins.get(team1Name) : 0;
                    
                    int totalMatches = team1WinsVal + team2WinsVal;
                    
                    System.out.println("H2H: Saving " + team1.getShortName() + " vs " + team2.getShortName() + " = " + team1WinsVal + "-" + team2WinsVal);
                    
                    HeadToHeadStats stats = h2hStatsRepository.findByTeamIds(team1.getId(), team2.getId())
                            .orElseGet(HeadToHeadStats::new);
                    
                    stats.setTeam1(team1);
                    stats.setTeam2(team2);
                    stats.setTotalMatches(totalMatches);
                    stats.setTeam1Wins(team1WinsVal);
                    stats.setTeam2Wins(team2WinsVal);
                    
                    h2hStatsRepository.save(stats);
                    importedCount++;
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading h2h file: " + e.getMessage());
            e.printStackTrace();
            throw new IOException(e);
        }
        return importedCount;
    }
    
    @Transactional
    public int importH2hStatsFromExcel(String filePath) throws IOException {
        int importedCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return 0;
            
            String[] teamColumns = headerLine.split(",");
            List<String> teamNames = new ArrayList<>();
            for (int i = 1; i < teamColumns.length; i++) {
                teamNames.add(teamColumns[i].trim());
            }
            
            Map<String, Team> teamMap = new HashMap<>();
            for (String shortName : teamNames) {
                teamRepository.findByShortName(shortName).ifPresent(team -> {
                    teamMap.put(shortName, team);
                });
            }
            
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 2) continue;
                
                try {
                    String team1Name = parts[0].trim();
                    Team team1 = teamMap.get(team1Name);
                    if (team1 == null) continue;
                    
                    for (int i = 1; i < parts.length && i <= teamNames.size(); i++) {
                        String team2Name = teamNames.get(i - 1);
                        if (team2Name.equals(team1Name)) continue;
                        
                        Team team2 = teamMap.get(team2Name);
                        if (team2 == null) continue;
                        
                        String value = parts[i].trim();
                        if (value.isEmpty() || value.equals("-")) continue;
                        
                        int team1Wins = Integer.parseInt(value);
                        
                        HeadToHeadStats stats = h2hStatsRepository.findByTeamIds(team1.getId(), team2.getId())
                                .orElse(new HeadToHeadStats());
                        
                        stats.setTeam1(team1);
                        stats.setTeam2(team2);
                        stats.setTotalMatches(team1Wins);
                        stats.setTeam1Wins(team1Wins);
                        stats.setTeam2Wins(team1Wins);
                        
                        h2hStatsRepository.save(stats);
                        importedCount++;
                    }
                } catch (Exception e) {
                    System.err.println("Error importing h2h row: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading h2h file: " + e.getMessage());
            throw new IOException(e);
        }
        return importedCount;
    }
    
    public HeadToHead getHeadToHeadFromDb(Long team1Id, Long team2Id) {
        return h2hStatsRepository.findByTeamIds(team1Id, team2Id)
                .map(h2h -> {
                    String team1Name = h2h.getTeam1().getTeamName();
                    String team2Name = h2h.getTeam2().getTeamName();
                    if (h2h.getTeam1().getId().equals(team1Id)) {
                        return new HeadToHead(h2h.getTotalMatches(), h2h.getTeam1Wins(), h2h.getTeam2Wins(), 0, team1Name, team2Name);
                    } else {
                        return new HeadToHead(h2h.getTotalMatches(), h2h.getTeam2Wins(), h2h.getTeam1Wins(), 0, team2Name, team1Name);
                    }
                })
                .orElse(null);
    }
    
    @Transactional
    public int importVenueStatsFromClasspath(String classpath) throws IOException {
        int importedCount = 0;
        
        InputStream is = getClass().getResourceAsStream(classpath);
        if (is == null) {
            throw new IOException("Resource not found: " + classpath);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return 0;
            
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 6) continue;
                
                try {
                    String stadium = parts[0].trim();
                    String city = parts[1].trim();
                    String pitchType = parts[2].trim();
                    int avgScore = Integer.parseInt(parts[3].trim());
                    int chasingWinPct = Integer.parseInt(parts[4].trim());
                    String dewFactor = parts[5].trim();
                    String boundarySize = parts.length > 6 ? parts[6].trim() : "medium";
                    
                    VenueStats stats = venueStatsRepository.findByStadium(stadium)
                            .orElse(new VenueStats());
                    
                    stats.setStadium(stadium);
                    stats.setCity(city);
                    stats.setPitchType(pitchType);
                    stats.setAvgScore(avgScore);
                    stats.setChasingWinPct(chasingWinPct);
                    stats.setDewFactor(dewFactor);
                    stats.setBoundarySize(boundarySize);
                    
                    venueStatsRepository.save(stats);
                    importedCount++;
                } catch (Exception e) {
                    System.err.println("Error importing venue row: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading venue file: " + e.getMessage());
            e.printStackTrace();
            throw new IOException(e);
        }
        return importedCount;
    }
    
    public VenueStats getVenueStatsByStadium(String stadium) {
        if (stadium == null || stadium.isBlank()) {
            return null;
        }
        
        String searchStadium = stadium;
        if (stadium.contains(",")) {
            searchStadium = stadium.split(",")[0].trim();
        }
        
        Optional<VenueStats> exactMatch = venueStatsRepository.findByStadium(searchStadium);
        if (exactMatch.isPresent()) {
            return exactMatch.get();
        }
        
        Optional<VenueStats> fuzzyMatch = venueStatsRepository.findByVenueContaining(searchStadium);
        return fuzzyMatch.orElse(null);
    }
    
private Long getTeamIdByName(String teamName) {
        return teamRepository.findByTeamNameOrShortName(teamName)
                .orElseGet(() -> {
                    Team team = new Team();
                    team.setTeamName(teamName);
                    String shortName = getShortNameForTeam(teamName);
                    team.setShortName(shortName);
                    team.setHomeCity("Unknown");
                    team.setStadium("Unknown");
                    team.setMatchesPlayed(0);
                    team.setMatchesWon(0);
                    team.setMatchesLost(0);
                    team.setNetRunRate(0.0);
                    team.setPoints(0);
                    return teamRepository.save(team);
                })
                .getId();
    }
    
    private String getShortNameForTeam(String teamName) {
        Map<String, String> teamShortNames = new HashMap<>();
        teamShortNames.put("Mumbai Indians", "MI");
        teamShortNames.put("Chennai Super Kings", "CSK");
        teamShortNames.put("Royal Challengers Bangalore", "RCB");
        teamShortNames.put("Kolkata Knight Riders", "KKR");
        teamShortNames.put("Delhi Capitals", "DC");
        teamShortNames.put("Sunrisers Hyderabad", "SRH");
        teamShortNames.put("Rajasthan Royals", "RR");
        teamShortNames.put("Punjab Kings", "PBKS");
        teamShortNames.put("Lucknow Super Giants", "LSG");
        teamShortNames.put("Gujarat Titans", "GT");
        teamShortNames.put("Kings XI Punjab", "PBKS");
        teamShortNames.put("Deccan Chargers", "SRH");
        teamShortNames.put("Delhi Daredevils", "DC");
        teamShortNames.put("Rising Pune Supergiant", "RPS");
        teamShortNames.put("Kochi Tuskers Kerala", "KTK");
        return teamShortNames.getOrDefault(teamName, teamName.substring(0, Math.min(3, teamName.length())).toUpperCase());
    }
    
    public Map<String, Object> predictMatch(String team1Name, String team2Name) {
        Optional<Team> team1Opt = teamRepository.findByTeamNameOrShortName(team1Name);
        Optional<Team> team2Opt = teamRepository.findByTeamNameOrShortName(team2Name);
        
        if (team1Opt.isEmpty() || team2Opt.isEmpty()) {
            throw new RuntimeException("One or both teams not found");
        }
        
        Team team1 = team1Opt.get();
        Team team2 = team2Opt.get();
        
        HeadToHead h2h = getHeadToHeadFromDb(team1.getId(), team2.getId());
        
        int team1Wins = 0;
        int team2Wins = 0;
        int totalMatches = 0;
        
        if (h2h != null) {
            totalMatches = h2h.getTotalMatches();
            team1Wins = h2h.getTeam1Wins();
            team2Wins = h2h.getTeam2Wins();
        }
        
        double team1Score = 50.0;
        double team2Score = 50.0;
        
        if (totalMatches > 0) {
            double h2hWeight = Math.min(totalMatches / 20.0, 0.5);
            double team1H2hPct = (double) team1Wins / totalMatches * 100;
            double team2H2hPct = (double) team2Wins / totalMatches * 100;
            
            team1Score = 50 + (team1H2hPct - 50) * h2hWeight;
            team2Score = 50 + (team2H2hPct - 50) * h2hWeight;
        }
        
        if (team1.getPoints() != null && team2.getPoints() != null) {
            double pointsDiff = team1.getPoints() - team2.getPoints();
            team1Score += pointsDiff * 0.5;
            team2Score -= pointsDiff * 0.5;
        }
        
        team1Score = Math.max(20, Math.min(80, team1Score));
        team2Score = Math.max(20, Math.min(80, team2Score));
        
        double total = team1Score + team2Score;
        int team1Prob = (int) Math.round(team1Score / total * 100);
        int team2Prob = 100 - team1Prob;
        
        String winner = team1Prob >= team2Prob ? team1.getTeamName() : team2.getTeamName();
        
        List<String> reasons = new ArrayList<>();
        
        if (h2h != null && totalMatches > 0) {
            if (team1Wins > team2Wins) {
                reasons.add(String.format("Historical advantage: %s has won %d of %d head-to-head matches", team1.getTeamName(), team1Wins, totalMatches));
            } else if (team2Wins > team1Wins) {
                reasons.add(String.format("Historical advantage: %s has won %d of %d head-to-head matches", team2.getTeamName(), team2Wins, totalMatches));
            } else {
                reasons.add("Head-to-head record is evenly split between the teams");
            }
        } else {
            reasons.add("No historical head-to-head data available");
        }
        
        if (team1.getPoints() != null && team2.getPoints() != null) {
            if (team1.getPoints() > team2.getPoints()) {
                reasons.add(String.format("%s has higher points (%d vs %d) in the standings", team1.getTeamName(), team1.getPoints(), team2.getPoints()));
            } else if (team2.getPoints() > team1.getPoints()) {
                reasons.add(String.format("%s has higher points (%d vs %d) in the standings", team2.getTeamName(), team2.getPoints(), team1.getPoints()));
            }
        }
        
        reasons.add(String.format("Probability analysis: %s at %d%%, %s at %d%%", team1.getTeamName(), team1Prob, team2.getTeamName(), team2Prob));
        
        Map<String, Object> result = new HashMap<>();
        result.put("team1", team1.getTeamName());
        result.put("team2", team2.getTeamName());
        result.put("winner", winner);
        result.put("team1Probability", team1Prob);
        result.put("team2Probability", team2Prob);
        result.put("reasons", reasons);
        result.put("headToHead", Map.of("totalMatches", totalMatches, "team1Wins", team1Wins, "team2Wins", team2Wins));
        
        return result;
    }
}