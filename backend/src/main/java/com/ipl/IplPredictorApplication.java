package com.ipl;

import com.ipl.service.MatchService;
import com.ipl.service.UserService;
import com.ipl.service.UserPointsService;
import com.ipl.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.InputStream;

@SpringBootApplication
@EntityScan("com.ipl.model")
@EnableJpaRepositories("com.ipl.repository")
@EnableScheduling
public class IplPredictorApplication implements CommandLineRunner {

    @Autowired
    private MatchService matchService;

@Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserPointsService userPointsService;

    public static void main(String[] args) {
        SpringApplication.run(IplPredictorApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            InputStream inputStream = getClass().getResourceAsStream("/data/matches.csv");
            if (inputStream == null) {
                System.err.println("Failed to find matches.csv in classpath");
                return;
            }
            matchService.importMatchesFromExcel(inputStream);
            System.out.println("Matches imported successfully from Excel.");
        } catch (Exception e) {
            System.err.println("Failed to import matches from Excel: " + e.getMessage());
            e.printStackTrace();
        }
        
        try {
            int h2hCount = matchService.importH2hStatsFromClasspath("/data/h2h_stats.csv");
            System.out.println(h2hCount + " head-to-head stats imported successfully.");
        } catch (Exception e) {
            System.err.println("Failed to import h2h stats: " + e.getMessage());
        }

        try {
            int venueCount = matchService.importVenueStatsFromClasspath("/data/venue.csv");
            System.out.println(venueCount + " venue stats imported successfully.");
        } catch (Exception e) {
            System.err.println("Failed to import venue stats: " + e.getMessage());
        }

        try {
            if (!userRepository.existsByUsername("admin")) {
                userService.registerUser("admin", null, "admin@ipl.com", "admin123", "Admin User", "ADMIN");
                System.out.println("Admin user created: admin / admin123");
            }
            if (!userRepository.existsByUsername("user")) {
                userService.registerUser("user", null, "user@ipl.com", "user123", "Test User", "USER");
                System.out.println("Test user created: user / user123");
            }
            if (!userRepository.existsByUsername("user1")) {
                userService.registerUser("user1", null, "user1@ipl.com", "user123", "John Smith", "USER");
                System.out.println("Test user created: user1 / user123");
            }
            if (!userRepository.existsByUsername("user2")) {
                userService.registerUser("user2", null, "user2@ipl.com", "user123", "Sarah Johnson", "USER");
                System.out.println("Test user created: user2 / user123");
            }
            if (!userRepository.existsByUsername("user3")) {
                userService.registerUser("user3", null, "user3@ipl.com", "user123", "Mike Williams", "USER");
                System.out.println("Test user created: user3 / user123");
            }
            if (!userRepository.existsByUsername("user4")) {
                userService.registerUser("user4", null, "user4@ipl.com", "user123", "Emily Brown", "USER");
                System.out.println("Test user created: user4 / user123");
            }
            if (!userRepository.existsByUsername("user5")) {
                userService.registerUser("user5", null, "user5@ipl.com", "user123", "David Lee", "USER");
                System.out.println("Test user created: user5 / user123");
            }
            
            try {
                userPointsService.syncAllUserPoints();
                System.out.println("User points synced to MongoDB");
            } catch (Exception e) {
                System.err.println("Failed to sync user points: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Failed to create default users: " + e.getMessage());
        }
    }
}