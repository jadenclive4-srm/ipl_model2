package com.ipl;

import com.ipl.repository.mongo.UserMongoRepository;
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
    private UserMongoRepository userMongoRepository;
    
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

        // Test MongoDB connection
        try {
            userMongoRepository.count();
            System.out.println("✅ MongoDB connection successful (users collection accessible)");
        } catch (Exception e) {
            System.err.println("❌ MongoDB connection FAILED: " + e.getMessage());
            System.err.println("   Users cannot register unless MongoDB is reachable.");
            System.err.println("   Check: MongoDB Atlas IP whitelist, credentials, network.");
        }

        try {
            // Reset admin to ensure active and verified (bypasses OTP)
            userService.resetAdminUser();

            // Sync all real users to userPoints collection
            try {
                userPointsService.syncAllUserPoints();
                System.out.println("User points synced to MongoDB");
            } catch (Exception e) {
                System.err.println("Failed to sync user points: " + e.getMessage());
            }

            // Log user authentication status and consolidate identities
            userService.logUserAuthenticationStatus();

            // Fix prediction usernames to match user accounts
            userService.fixPredictionUsernames();

            // Clean up orphaned predictions
            userService.cleanupOrphanedPredictions();

        } catch (Exception e) {
            System.err.println("Failed to create default users: " + e.getMessage());
        }
    }
}