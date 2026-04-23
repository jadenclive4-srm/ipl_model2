package com.ipl.service;

import com.ipl.model.EmailVerificationToken;
import com.ipl.model.User;
import com.ipl.model.mongo.UserMongo;
import com.ipl.repository.EmailVerificationRepository;
import com.ipl.repository.UserRepository;
import com.ipl.repository.mongo.UserMongoRepository;
import com.ipl.service.UserPointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;
    private final UserMongoRepository userMongoRepository;
    private final UserPointsService userPointsService;
    
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int OTP_LENGTH = 6;
    private final Random random = new Random();
    
    @Transactional
    public User registerUser(String username, String uniqueUserId, String email, String password, String fullName, String role) {
        // Generate uniqueUserId if not provided
        if (uniqueUserId == null || uniqueUserId.trim().isEmpty()) {
            uniqueUserId = UUID.randomUUID().toString();
        }

        log.info("Registering user: username={}, email={}, role={}", username, email, role);
        
        // Check H2 separately first
        boolean existsInH2 = userRepository.existsByUsername(username);
        boolean existsInMongo = false;
        
        // Check MongoDB separately with error handling
        try {
            existsInMongo = userMongoRepository.existsByUsername(username);
        } catch (Exception e) {
            log.error("MongoDB check failed for username: {}", username, e);
            throw new RuntimeException("Database error: Unable to verify username availability. Please try again later.");
        }
        
        if (existsInH2 || existsInMongo) {
            throw new RuntimeException("Username already exists");
        }
        
        // Check duplicate email in both stores (separately with error handling)
        boolean emailExistsInH2 = userRepository.existsByEmail(email);
        boolean emailExistsInMongo = false;
        try {
            emailExistsInMongo = userMongoRepository.existsByEmail(email);
        } catch (Exception e) {
            log.error("MongoDB check failed for email: {}", email, e);
            throw new RuntimeException("Database error: Unable to verify email availability. Please try again later.");
        }
        
        if (emailExistsInH2 || emailExistsInMongo) {
            throw new RuntimeException("Email already exists");
        }

        // Create H2 user
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setUniqueUserId(uniqueUserId);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(role);
        user.setIsActive(false);
        user.setEmailVerified(false);
        user.setCreatedAt(System.currentTimeMillis());
        user.setUpdatedAt(System.currentTimeMillis());

        User savedUser = userRepository.save(user);

        // Send verification OTP
        generateAndSendVerificationOtp(email);
        
        log.info("User registered successfully (H2 only, pending OTP): id={}, username={}, email={}", 
            savedUser.getId(), savedUser.getUsername(), savedUser.getEmail());
        
        return savedUser;
    }

    @Transactional
    public void resetAdminUser() {
        // Delete existing admin from both stores
        userRepository.findByUsername("admin").ifPresent(user -> {
            userRepository.delete(user);
            System.out.println("Deleted existing admin from H2");
        });
        try {
            userMongoRepository.findByUsername("admin").ifPresent(userMongo -> {
                userMongoRepository.delete(userMongo);
                System.out.println("Deleted existing admin from MongoDB");
            });
        } catch (Exception e) {
            System.err.println("Warning: Could not delete admin from MongoDB: " + e.getMessage());
        }

        // Create fresh admin without OTP
        try {
            createUserWithoutOtp("admin", null, "admin@ipl.com", "admin123", "Admin User", "ADMIN");
            System.out.println("Admin user (re)created: admin / admin123");
        } catch (Exception e) {
            System.err.println("Failed to create admin user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public User createUserWithoutOtp(String username, String uniqueUserId, String email, String password, String fullName, String role) {
        // Generate uniqueUserId if not provided
        if (uniqueUserId == null || uniqueUserId.trim().isEmpty()) {
            uniqueUserId = UUID.randomUUID().toString();
        }

        // Check H2 separately
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists in H2");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists in H2");
        }

        // Check MongoDB separately with error handling
        boolean mongoUsernameExists = false;
        boolean mongoEmailExists = false;
        try {
            mongoUsernameExists = userMongoRepository.existsByUsername(username);
            mongoEmailExists = userMongoRepository.existsByEmail(email);
        } catch (Exception e) {
            log.error("MongoDB check failed during user creation: username={}, email={}", username, email, e);
            throw new RuntimeException("Database error: MongoDB is unreachable. Please check MongoDB connection.");
        }
        
        if (mongoUsernameExists) {
            throw new RuntimeException("Username already exists in MongoDB");
        }
        if (mongoEmailExists) {
            throw new RuntimeException("Email already exists in MongoDB");
        }

        // Create H2 user - ACTIVATED and VERIFIED
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setUniqueUserId(uniqueUserId);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(role);
        user.setIsActive(true);
        user.setEmailVerified(true);
        user.setCreatedAt(System.currentTimeMillis());
        user.setUpdatedAt(System.currentTimeMillis());

        User savedUser = userRepository.save(user);

        // Create MongoDB user - ACTIVATED and VERIFIED
        UserMongo userMongo = new UserMongo();
        userMongo.setId(savedUser.getId());
        userMongo.setUsername(username);
        userMongo.setEmail(email);
        userMongo.setUniqueUserId(uniqueUserId);
        userMongo.setPassword(savedUser.getPassword());
        userMongo.setFullName(fullName);
        userMongo.setPoints(0);
        userMongo.setRank(0);
        userMongo.setIsActive(true);
        userMongo.setEmailVerified(true);
        userMongo.setRole(role);
        userMongo.setCreatedAt(savedUser.getCreatedAt());
        userMongo.setUpdatedAt(savedUser.getUpdatedAt());
        userMongoRepository.save(userMongo);

        return savedUser;
    }
    
    public Optional<User> findByUsername(String username) {
        // First check H2
        Optional<User> h2User = userRepository.findByUsername(username);
        if (h2User.isPresent()) {
            return h2User;
        }
        
         // Check MongoDB and convert to H2 User
         try {
             Optional<UserMongo> mongoUser = userMongoRepository.findByUsername(username);
             if (mongoUser.isPresent()) {
                 UserMongo um = mongoUser.get();
                 User user = new User();
                 user.setId(um.getId());
                 user.setUsername(um.getUsername());
                 user.setEmail(um.getEmail());
                 user.setUniqueUserId(um.getUniqueUserId());
                 user.setPassword(um.getPassword());
                 user.setFullName(um.getFullName());
                 user.setRole(um.getRole());
                 user.setIsActive(um.getIsActive());
                 user.setEmailVerified(um.getEmailVerified());
                 user.setCreatedAt(um.getCreatedAt());
                 user.setUpdatedAt(um.getUpdatedAt());
                 return Optional.of(user);
             }
         } catch (Exception e) {
             System.err.println("Error checking MongoDB for user: " + e.getMessage());
         }
        
        return Optional.empty();
    }
    
    public Optional<User> findById(Long id) {
        // First check H2
        Optional<User> h2User = userRepository.findById(id);
        if (h2User.isPresent()) {
            return h2User;
        }
        
         // Check MongoDB and sync to H2
         try {
             Optional<UserMongo> mongoUser = userMongoRepository.findById(id);
             if (mongoUser.isPresent()) {
                 UserMongo um = mongoUser.get();
                 User user = new User();
                 user.setId(um.getId());
                 user.setUsername(um.getUsername());
                 user.setEmail(um.getEmail());
                 user.setUniqueUserId(um.getUniqueUserId());
                 user.setPassword(um.getPassword());
                 user.setFullName(um.getFullName());
                 user.setRole(um.getRole());
                 user.setIsActive(um.getIsActive());
                 user.setEmailVerified(um.getEmailVerified());
                 user.setCreatedAt(um.getCreatedAt());
                 user.setUpdatedAt(um.getUpdatedAt());
                 
                 // Save to H2 so foreign key constraints work
                 User savedUser = userRepository.save(user);
                 return Optional.of(savedUser);
             }
         } catch (Exception e) {
             System.err.println("Error checking MongoDB for user id: " + e.getMessage());
         }
        
        return Optional.empty();
    }
     
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("loadUserByUsername called for: " + username);
        
        // Try MongoDB first
        Optional<UserMongo> mongoOpt;
        try {
            mongoOpt = userMongoRepository.findByUsername(username);
            System.out.println("MongoDB lookup result for " + username + ": " + (mongoOpt.isPresent() ? "found" : "not found"));
        } catch (Exception e) {
            System.err.println("MongoDB lookup failed for " + username + ": " + e.getMessage());
            mongoOpt = Optional.empty();
        }
        
        if (mongoOpt.isPresent()) {
            UserMongo mongoUser = mongoOpt.get();
            System.out.println("Loading user from MongoDB: " + username + ", isActive=" + mongoUser.getIsActive() + ", hasPassword=" + (mongoUser.getPassword() != null));
            return new org.springframework.security.core.userdetails.User(
                mongoUser.getUsername(),
                mongoUser.getPassword(),
                mongoUser.getIsActive() != null ? mongoUser.getIsActive() : true,
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + mongoUser.getRole()))
            );
        }
        
        // Fallback to H2 (for backward compatibility or migration)
        Optional<User> h2Opt = userRepository.findByUsername(username);
        System.out.println("H2 lookup result for " + username + ": " + (h2Opt.isPresent() ? "found" : "not found"));
        
        if (h2Opt.isEmpty()) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        User h2User = h2Opt.get();
        
        System.out.println("Loading user from H2: " + username + ", isActive=" + h2User.getIsActive() + ", hasPassword=" + (h2User.getPassword() != null));
        
         // Migrate to MongoDB
         UserMongo migrated = new UserMongo();
         migrated.setId(h2User.getId());
         migrated.setUsername(h2User.getUsername());
         migrated.setEmail(h2User.getEmail());
         migrated.setUniqueUserId(h2User.getUniqueUserId());
         migrated.setPassword(h2User.getPassword());
         migrated.setFullName(h2User.getFullName());
         migrated.setIsActive(h2User.getIsActive());
         migrated.setEmailVerified(h2User.getEmailVerified() != null ? h2User.getEmailVerified() : false);
         migrated.setRole(h2User.getRole());
         migrated.setCreatedAt(h2User.getCreatedAt());
         migrated.setUpdatedAt(h2User.getUpdatedAt() != null ? h2User.getUpdatedAt() : System.currentTimeMillis());
        try {
            userMongoRepository.save(migrated);
            System.out.println("User migrated to MongoDB: " + username);
        } catch (Exception e) {
            System.err.println("Migration to MongoDB failed: " + e.getMessage());
        }
        
        return new org.springframework.security.core.userdetails.User(
            h2User.getUsername(),
            h2User.getPassword(),
            h2User.getIsActive() != null ? h2User.getIsActive() : true,
            true,
            true,
            true,
            List.of(new SimpleGrantedAuthority("ROLE_" + h2User.getRole()))
        );
     }
      
    @Transactional
    public void updateUserPoints(Long userId, Integer pointsToAdd) {
        userPointsService.updateUserPoints(userId, pointsToAdd);
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    @Transactional
    public User updateUser(Long userId, String fullName, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (fullName != null) {
            user.setFullName(fullName);
        }
        if (email != null && !email.equals(user.getEmail())) {
            boolean existsH2 = userRepository.existsByEmail(email);
            boolean existsMongo = userMongoRepository.existsByEmail(email);
            if (existsH2 || existsMongo) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(email);
        }
        
        user.setUpdatedAt(System.currentTimeMillis());
        user = userRepository.save(user);
        
        // Update MongoDB
        UserMongo userMongo = userMongoRepository.findById(userId).orElse(null);
        if (userMongo != null) {
            if (fullName != null) {
                userMongo.setFullName(fullName);
            }
            if (email != null) {
                userMongo.setEmail(email);
            }
            userMongo.setUpdatedAt(user.getUpdatedAt());
            userMongoRepository.save(userMongo);
        } else {
            // Migrate missing MongoDB entry
            UserMongo migrated = new UserMongo();
            migrated.setId(userId);
            migrated.setUsername(user.getUsername());
            migrated.setEmail(user.getEmail());
            migrated.setUniqueUserId(user.getUniqueUserId());
            migrated.setPassword(user.getPassword());
            migrated.setFullName(user.getFullName());
            migrated.setIsActive(user.getIsActive());
            migrated.setEmailVerified(user.getEmailVerified() != null ? user.getEmailVerified() : false);
            migrated.setRole(user.getRole());
            migrated.setCreatedAt(user.getCreatedAt());
            migrated.setUpdatedAt(user.getUpdatedAt());
            userMongoRepository.save(migrated);
        }
        
        return user;
    }
    
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setIsActive(false);
        user.setUpdatedAt(System.currentTimeMillis());
        userRepository.save(user);
        
        // Update MongoDB
        UserMongo userMongo = userMongoRepository.findById(userId).orElse(null);
        if (userMongo != null) {
            userMongo.setIsActive(false);
            userMongo.setUpdatedAt(user.getUpdatedAt());
            userMongoRepository.save(userMongo);
        }
        // If missing, ignore (could be legacy)
    }

    @Transactional
    public void updatePassword(Long userId, String newPassword) {
        String encoded = passwordEncoder.encode(newPassword);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(encoded);
        user.setUpdatedAt(System.currentTimeMillis());
        userRepository.save(user);
        
        // Update MongoDB
        UserMongo userMongo = userMongoRepository.findById(userId).orElse(null);
        if (userMongo != null) {
            userMongo.setPassword(encoded);
            userMongo.setUpdatedAt(user.getUpdatedAt());
            userMongoRepository.save(userMongo);
        } else {
            // Migrate
            userMongo = new UserMongo();
            userMongo.setId(userId);
            userMongo.setUsername(user.getUsername());
            userMongo.setEmail(user.getEmail());
            userMongo.setUniqueUserId(user.getUniqueUserId());
            userMongo.setPassword(encoded);
            userMongo.setFullName(user.getFullName());
            userMongo.setIsActive(user.getIsActive());
            userMongo.setEmailVerified(user.getEmailVerified() != null ? user.getEmailVerified() : false);
            userMongo.setRole(user.getRole());
            userMongo.setCreatedAt(user.getCreatedAt());
            userMongo.setUpdatedAt(user.getUpdatedAt());
            userMongoRepository.save(userMongo);
        }
    }

    // Generate a 6-digit OTP
    private String generateOtp() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    // Generate and send verification OTP
    @Transactional
    public void generateAndSendVerificationOtp(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found with email: " + email);
        }
        
        // Delete any existing unused tokens for this email
        emailVerificationRepository.deleteByEmail(email);
        
        String otp = generateOtp();
        String tokenHash = passwordEncoder.encode(otp);
        long expiresAt = Instant.now().plusSeconds(OTP_EXPIRY_MINUTES * 60L).toEpochMilli();
        
        EmailVerificationToken token = new EmailVerificationToken();
        token.setEmail(email);
        token.setTokenHash(tokenHash);
        token.setExpiresAt(expiresAt);
        token.setUsed(false);
        token.setCreatedAt(Instant.now().toEpochMilli());
        
        emailVerificationRepository.save(token);
        
        log.info("OTP generated and saved for email: {}, expiresAt: {}", email, expiresAt);
        
        try {
            emailService.sendVerificationOtpEmail(email, otp);
            log.info("OTP sent to email: {}", email);
        } catch (Exception e) {
            // Log OTP so user can still verify even if email fails
            log.warn("==============================================");
            log.warn("EMAIL SERVICE FAILED - OTP for {} is: {}", email, otp);
            log.warn("User can enter this OTP manually in the UI");
            log.warn("==============================================");
            // Don't throw - allow registration to continue
        }
    }

    // Verify OTP and activate user account
    @Transactional
    public User verifyOtp(String email, String otp) {
        EmailVerificationToken token = emailVerificationRepository.findByEmailAndUsedFalse(email)
                .orElseThrow(() -> new RuntimeException("Invalid or expired OTP"));
        
        // Check expiry
        if (token.getExpiresAt() <= Instant.now().toEpochMilli()) {
            throw new RuntimeException("OTP has expired");
        }
        
        // Verify OTP
        if (!passwordEncoder.matches(otp, token.getTokenHash())) {
            throw new RuntimeException("Invalid OTP");
        }
        
        // Mark token as used
        token.setUsed(true);
        emailVerificationRepository.save(token);
        
        // Update H2 user - activate and verify
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found in H2 database"));
        user.setEmailVerified(true);
        user.setIsActive(true);
        user.setUpdatedAt(Instant.now().toEpochMilli());
        userRepository.save(user);
        
        log.info("H2 user activated: id={}, username={}", user.getId(), user.getUsername());

        // Create/update MongoDB user (only after OTP verification)
        try {
            Optional<UserMongo> existingMongo = userMongoRepository.findByEmail(email);
            if (existingMongo.isEmpty()) {
                UserMongo userMongo = new UserMongo();
                userMongo.setId(user.getId());
                userMongo.setUsername(user.getUsername());
                userMongo.setEmail(user.getEmail());
                userMongo.setUniqueUserId(user.getUniqueUserId());
                userMongo.setPassword(user.getPassword());
                userMongo.setFullName(user.getFullName());
                userMongo.setIsActive(true);
                userMongo.setEmailVerified(true);
                userMongo.setRole(user.getRole());
                userMongo.setCreatedAt(user.getCreatedAt());
                userMongo.setUpdatedAt(user.getUpdatedAt());
                userMongoRepository.save(userMongo);
                log.info("MongoDB user created after OTP verification: email={}", email);
            } else {
                // Update existing if somehow present
                UserMongo userMongo = existingMongo.get();
                userMongo.setIsActive(true);
                userMongo.setEmailVerified(true);
                userMongo.setUpdatedAt(Instant.now().toEpochMilli());
                userMongoRepository.save(userMongo);
                log.info("MongoDB user updated after OTP verification: email={}", email);
            }
        } catch (Exception e) {
            log.error("Failed to create/update MongoDB user during OTP verification: email={}", email, e);
            // Don't rollback H2 changes; user is verified in H2. MongoDB may be down.
            // The user can still log in ( auth uses both, but H2 has isActive=true )
            // For now, we'll let the exception propagate so user knows verification succeeded but system issue
            // TODO: Consider queuing MongoDB sync for later
        }
        
        log.info("Email verified for user: {}", email);
        
        return user;
    }

    // Resend OTP
    @Transactional
    public void resendVerificationOtp(String email) {
        // Simply generate and send new OTP (old ones will be deleted)
        generateAndSendVerificationOtp(email);
    }
}