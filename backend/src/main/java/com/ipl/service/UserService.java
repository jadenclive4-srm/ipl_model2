package com.ipl.service;

import com.ipl.model.EmailVerificationToken;
import com.ipl.model.User;
import com.ipl.model.mongo.UserMongo;
import com.ipl.repository.EmailVerificationRepository;
import com.ipl.repository.UserRepository;
import com.ipl.repository.mongo.UserMongoRepository;
import com.ipl.repository.mongo.UserPredictionRepository;
import com.ipl.service.UserPointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
@Slf4j
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;
    private final UserMongoRepository userMongoRepository;
    private final UserPointsService userPointsService;
    private final UserPredictionRepository userPredictionRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                      EmailVerificationRepository emailVerificationRepository, EmailService emailService,
                      UserMongoRepository userMongoRepository, UserPointsService userPointsService,
                      UserPredictionRepository userPredictionRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationRepository = emailVerificationRepository;
        this.emailService = emailService;
        this.userMongoRepository = userMongoRepository;
        this.userPointsService = userPointsService;
        this.userPredictionRepository = userPredictionRepository;
    }
    
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
            log.info("Deleted existing admin from H2");
        });
        try {
            userMongoRepository.findByUsername("admin").ifPresent(userMongo -> {
                userMongoRepository.delete(userMongo);
                log.info("Deleted existing admin from MongoDB");
            });
        } catch (Exception e) {
            log.warn("Warning: Could not delete admin from MongoDB: {}", e.getMessage());
        }

        // Create fresh admin without OTP
        try {
            createUserWithoutOtp("admin", null, "admin@ipl.com", "admin123", "Admin User", "ADMIN");
            log.info("Admin user (re)created: admin / admin123");
        } catch (Exception e) {
            log.error("Failed to create admin user: {}", e.getMessage());
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

    @Transactional
    public UserMongo createUserMongoOnly(String username, String uniqueUserId, String email, String password, String fullName, String role) {
        // Generate uniqueUserId if not provided
        if (uniqueUserId == null || uniqueUserId.trim().isEmpty()) {
            uniqueUserId = UUID.randomUUID().toString();
        }

        // Check MongoDB for duplicates
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

        // Generate unique ID for MongoDB (using timestamp + random)
        long generatedId = System.currentTimeMillis() + new Random().nextInt(1000);

        // Create MongoDB user only
        UserMongo userMongo = new UserMongo();
        userMongo.setId(generatedId);
        userMongo.setUsername(username);
        userMongo.setEmail(email);
        userMongo.setUniqueUserId(uniqueUserId);
        userMongo.setPassword(passwordEncoder.encode(password));
        userMongo.setFullName(fullName);
        userMongo.setPoints(0);
        userMongo.setRank(0);
        userMongo.setIsActive(true);
        userMongo.setEmailVerified(true);
        userMongo.setRole(role != null ? role : "USER");
        userMongo.setCreatedAt(System.currentTimeMillis());
        userMongo.setUpdatedAt(System.currentTimeMillis());

        UserMongo savedUser = userMongoRepository.save(userMongo);

        // Also create UserPoints entry so user appears in leaderboard with 0 points
        try {
            userPointsService.updateUserPoints(savedUser.getId(), 0);
            log.info("Created UserPoints entry for new user: {}", savedUser.getUsername());
        } catch (Exception e) {
            log.warn("Failed to create UserPoints entry for user {}: {}", savedUser.getUsername(), e.getMessage());
        }

        log.info("User created in MongoDB only: id={}, username={}, email={}",
            savedUser.getId(), savedUser.getUsername(), savedUser.getEmail());

        return savedUser;
    }

    public Optional<User> findByUsername(String username) {
        // Prioritize MongoDB for consistency (canonical source)
        try {
            Optional<UserMongo> mongoUser = userMongoRepository.findByUsername(username);
            if (mongoUser.isPresent()) {
                UserMongo um = mongoUser.get();
                User user = new User();
                user.setId(um.getId()); // Use MongoDB ID as canonical
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
            log.warn("MongoDB lookup failed for user {}, trying H2: {}", username, e.getMessage());
        }

        // Fallback to H2 for backward compatibility
        Optional<User> h2User = userRepository.findByUsername(username);
        if (h2User.isPresent()) {
            log.debug("Found user {} in H2 only (ID: {})", username, h2User.get().getId());
            return h2User;
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

                  // Check if H2 already has a user with this username (data inconsistency)
                  Optional<User> existingH2User = userRepository.findByUsername(um.getUsername());
                  if (existingH2User.isPresent()) {
                      log.warn("Found H2 user with same username '{}' as MongoDB user ID {}. Using existing H2 user ID {} instead of syncing.",
                          um.getUsername(), um.getId(), existingH2User.get().getId());
                      return existingH2User;
                  }

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
               log.error("Error checking MongoDB for user id: {}", e.getMessage());
           }
        
        return Optional.empty();
    }
     
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("loadUserByUsername called for: {}", username);

        // Try MongoDB first
        Optional<UserMongo> mongoOpt;
        try {
            mongoOpt = userMongoRepository.findByUsername(username);
            log.debug("MongoDB lookup result for {}: {}", username, (mongoOpt.isPresent() ? "found" : "not found"));
        } catch (Exception e) {
            log.error("MongoDB lookup failed for {}: {}", username, e.getMessage());
            mongoOpt = Optional.empty();
        }

        if (mongoOpt.isPresent()) {
            UserMongo mongoUser = mongoOpt.get();
            log.debug("Loading user from MongoDB: {}, isActive={}, hasPassword={}", username, mongoUser.getIsActive(), (mongoUser.getPassword() != null));
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
        log.debug("H2 lookup result for {}: {}", username, (h2Opt.isPresent() ? "found" : "not found"));

        if (h2Opt.isEmpty()) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        User h2User = h2Opt.get();

        log.debug("Loading user from H2: {}, isActive={}, hasPassword={}", username, h2User.getIsActive(), (h2User.getPassword() != null));

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
            log.debug("User migrated to MongoDB: {}", username);
        } catch (Exception e) {
            log.error("Migration to MongoDB failed: {}", e.getMessage());
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

    /**
     * Get the currently authenticated user from SecurityContext
     * Always returns user with canonical MongoDB userId for consistency
     */
    public User getCurrentAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // First try MongoDB for canonical user
        try {
            Optional<UserMongo> mongoUser = userMongoRepository.findByUsername(username);
            if (mongoUser.isPresent()) {
                UserMongo um = mongoUser.get();
                User user = new User();
                user.setId(um.getId()); // Use MongoDB ID as canonical
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
                return user;
            }
        } catch (Exception e) {
            log.warn("MongoDB lookup failed for user {}, falling back to H2: {}", username, e.getMessage());
        }

        // Fallback to H2 - but this should be rare for active users
        Optional<User> h2User = userRepository.findByUsername(username);
        if (h2User.isPresent()) {
            User user = h2User.get();
            log.warn("Using H2 user for {} (ID: {}) - should migrate to MongoDB", username, user.getId());

            // Try to migrate to MongoDB on the fly
            try {
                Optional<UserMongo> existingMongo = userMongoRepository.findByUsername(username);
                if (!existingMongo.isPresent()) {
                    UserMongo migrated = new UserMongo();
                    migrated.setId(user.getId()); // Keep H2 ID for compatibility
                    migrated.setUsername(user.getUsername());
                    migrated.setEmail(user.getEmail());
                    migrated.setUniqueUserId(user.getUniqueUserId());
                    migrated.setPassword(user.getPassword());
                    migrated.setFullName(user.getFullName());
                    migrated.setIsActive(user.getIsActive());
                    migrated.setEmailVerified(user.getEmailVerified());
                    migrated.setRole(user.getRole());
                    migrated.setCreatedAt(user.getCreatedAt());
                    migrated.setUpdatedAt(user.getUpdatedAt());
                    userMongoRepository.save(migrated);
                    log.info("Migrated H2 user {} to MongoDB", username);
                }
            } catch (Exception e) {
                log.error("Failed to migrate user {} to MongoDB: {}", username, e.getMessage());
            }

            return user;
        }

        throw new RuntimeException("Authenticated user not found: " + username);
    }

    /**
     * Get authentication status summary for all users
     */
    public void logUserAuthenticationStatus() {
        try {
            // Count H2 users
            long totalH2Users = userRepository.count();
            long activeH2Users = userRepository.findAll().stream()
                    .filter(u -> Boolean.TRUE.equals(u.getIsActive()) && Boolean.TRUE.equals(u.getEmailVerified()))
                    .count();
            long inactiveH2Users = totalH2Users - activeH2Users;

            // Count MongoDB users
            long totalMongoUsers = userMongoRepository.count();
            long activeMongoUsers = userMongoRepository.findAll().stream()
                    .filter(u -> Boolean.TRUE.equals(u.getIsActive()) && Boolean.TRUE.equals(u.getEmailVerified()))
                    .count();
            long inactiveMongoUsers = totalMongoUsers - activeMongoUsers;

            log.info("=== USER AUTHENTICATION STATUS ===");
            log.info("H2 Database:");
            log.info("  Total users: {}", totalH2Users);
            log.info("  Authenticated (active + verified): {}", activeH2Users);
            log.info("  Not authenticated: {}", inactiveH2Users);

            log.info("MongoDB Database:");
            log.info("  Total users: {}", totalMongoUsers);
            log.info("  Authenticated (active + verified): {}", activeMongoUsers);
            log.info("  Not authenticated: {}", inactiveMongoUsers);

            // Check uniqueUserId status
            long h2UsersWithUniqueId = userRepository.findAll().stream()
                    .filter(u -> u.getUniqueUserId() != null && !u.getUniqueUserId().trim().isEmpty())
                    .count();
            long mongoUsersWithUniqueId = userMongoRepository.findAll().stream()
                    .filter(u -> u.getUniqueUserId() != null && !u.getUniqueUserId().trim().isEmpty())
                    .count();

            log.info("Combined Totals:");
            log.info("  Total users: {}", totalH2Users + totalMongoUsers);
            log.info("  Authenticated: {}", activeH2Users + activeMongoUsers);
            log.info("  Not authenticated: {}", inactiveH2Users + inactiveMongoUsers);
            log.info("Unique User ID Status:");
            log.info("  H2 users with uniqueUserId: {} / {}", h2UsersWithUniqueId, totalH2Users);
            log.info("  MongoDB users with uniqueUserId: {} / {}", mongoUsersWithUniqueId, totalMongoUsers);
            log.info("  Total users with uniqueUserId: {} / {}", h2UsersWithUniqueId + mongoUsersWithUniqueId, totalH2Users + totalMongoUsers);

            // Check prediction userIds
            try {
                long totalPredictions = 0;
                java.util.Set<Long> uniquePredictionUserIds = new java.util.HashSet<>();
                java.util.Map<Long, Integer> predictionCountByUser = new java.util.HashMap<>();

                // Get all predictions from MongoDB
                java.util.List<com.ipl.model.mongo.UserPrediction> allPredictions =
                    userPredictionRepository.findAll();

                for (com.ipl.model.mongo.UserPrediction prediction : allPredictions) {
                    totalPredictions++;
                    uniquePredictionUserIds.add(prediction.getUserId());
                    predictionCountByUser.put(prediction.getUserId(),
                        predictionCountByUser.getOrDefault(prediction.getUserId(), 0) + 1);
                }

                log.info("Prediction Analysis:");
                log.info("  Total predictions: {}", totalPredictions);
                log.info("  Unique userIds in predictions: {}", uniquePredictionUserIds.size());
                log.info("  UserIds with predictions: {}", uniquePredictionUserIds);

                for (java.util.Map.Entry<Long, Integer> entry : predictionCountByUser.entrySet()) {
                    log.info("  UserId {}: {} predictions", entry.getKey(), entry.getValue());
                }

                // Check if prediction userIds correspond to actual users
                log.info("UserId Validation:");
                java.util.Set<Long> allValidUserIds = new java.util.HashSet<>();
                java.util.Map<Long, String> userIdToUsername = new java.util.HashMap<>();
                java.util.Map<String, java.util.List<Long>> usernameToUserIds = new java.util.HashMap<>();

                // Get all H2 users
                java.util.List<User> h2Users = userRepository.findAll();
                for (User user : h2Users) {
                    allValidUserIds.add(user.getId());
                    userIdToUsername.put(user.getId(), user.getUsername());
                    usernameToUserIds.computeIfAbsent(user.getUsername(), k -> new java.util.ArrayList<>()).add(user.getId());
                }

                // Get all MongoDB users
                java.util.List<UserMongo> mongoUsers = userMongoRepository.findAll();
                for (UserMongo user : mongoUsers) {
                    allValidUserIds.add(user.getId());
                    userIdToUsername.put(user.getId(), user.getUsername());
                    usernameToUserIds.computeIfAbsent(user.getUsername(), k -> new java.util.ArrayList<>()).add(user.getId());
                }

                log.info("  Total valid userIds in system: {}", allValidUserIds.size());
                log.info("  Valid userIds: {}", allValidUserIds);

                // Check for duplicate usernames
                log.info("Username Duplication Analysis:");
                boolean hasDuplicateUsernames = false;
                for (java.util.Map.Entry<String, java.util.List<Long>> entry : usernameToUserIds.entrySet()) {
                    String username = entry.getKey();
                    java.util.List<Long> userIds = entry.getValue();
                    if (userIds.size() > 1) {
                        hasDuplicateUsernames = true;
                        log.error("  ❌ CRITICAL: Username '{}' used by multiple userIds: {}", username, userIds);
                    }
                }

                if (!hasDuplicateUsernames) {
                    log.info("  ✅ No duplicate usernames found");
                }

                // Check prediction username consistency
                log.info("Prediction Username Consistency:");
                for (com.ipl.model.mongo.UserPrediction prediction : allPredictions) {
                    Long predUserId = prediction.getUserId();
                    String predUsername = prediction.getUsername();
                    String expectedUsername = userIdToUsername.get(predUserId);

                    if (expectedUsername != null && !expectedUsername.equals(predUsername)) {
                        log.error("  ❌ CRITICAL: Prediction userId={} has username='{}' but user has username='{}'",
                            predUserId, predUsername, expectedUsername);
                    }
                }

                // Find invalid prediction userIds
                java.util.Set<Long> invalidPredictionUserIds = new java.util.HashSet<>();
                for (Long predUserId : uniquePredictionUserIds) {
                    if (!allValidUserIds.contains(predUserId)) {
                        invalidPredictionUserIds.add(predUserId);
                    }
                }

                if (!invalidPredictionUserIds.isEmpty()) {
                    log.error("  ❌ CRITICAL: Found predictions with INVALID userIds: {}", invalidPredictionUserIds);
                    log.error("  This indicates predictions are being created with wrong userIds!");
                } else {
                    log.info("  ✅ All prediction userIds are valid");
                }

            } catch (Exception e) {
                log.error("Error analyzing predictions: {}", e.getMessage());
            }

            // Fix user ID inconsistencies
            consolidateUserIdentities();
        } catch (Exception e) {
            log.error("Error checking user authentication status: {}", e.getMessage());
        }
    }

    /**
     * Fix prediction usernames to match their actual user accounts
     */
    public void fixPredictionUsernames() {
        try {
            log.info("=== FIXING PREDICTION USERNAMES ===");

            java.util.List<com.ipl.model.mongo.UserPrediction> allPredictions =
                userPredictionRepository.findAll();

            java.util.Map<Long, String> userIdToUsername = new java.util.HashMap<>();

            // Build userId to username mapping from all users
            java.util.List<User> h2Users = userRepository.findAll();
            for (User user : h2Users) {
                userIdToUsername.put(user.getId(), user.getUsername());
            }

            java.util.List<UserMongo> mongoUsers = userMongoRepository.findAll();
            for (UserMongo user : mongoUsers) {
                userIdToUsername.put(user.getId(), user.getUsername());
            }

            int fixedCount = 0;
            for (com.ipl.model.mongo.UserPrediction prediction : allPredictions) {
                Long userId = prediction.getUserId();
                String currentUsername = prediction.getUsername();
                String correctUsername = userIdToUsername.get(userId);

                if (correctUsername != null && !correctUsername.equals(currentUsername)) {
                    log.info("Fixing prediction {}: userId={} username '{}' -> '{}'",
                        prediction.getId(), userId, currentUsername, correctUsername);
                    prediction.setUsername(correctUsername);
                    userPredictionRepository.save(prediction);
                    fixedCount++;
                }
            }

            log.info("Fixed {} predictions with incorrect usernames", fixedCount);

        } catch (Exception e) {
            log.error("Error fixing prediction usernames: {}", e.getMessage());
        }
    }

    /**
     * Consolidate user identities and fix prediction userIds
     */
    public void consolidateUserIdentities() {
        try {
            log.info("=== CONSOLIDATING USER IDENTITIES ===");

            // Step 1: Build user ID mapping (H2 ID -> MongoDB ID)
            java.util.Map<Long, Long> userIdMapping = new java.util.HashMap<>();

            // Get all H2 users and map them to their MongoDB equivalents
            java.util.List<User> h2Users = userRepository.findAll();
            for (User h2User : h2Users) {
                try {
                    // Try to find MongoDB equivalent by username
                    Optional<UserMongo> mongoUser = userMongoRepository.findByUsername(h2User.getUsername());
                    if (mongoUser.isPresent()) {
                        userIdMapping.put(h2User.getId(), mongoUser.get().getId());
                        log.info("Mapped H2 user {} (ID: {}) -> MongoDB user {} (ID: {})",
                            h2User.getUsername(), h2User.getId(), mongoUser.get().getUsername(), mongoUser.get().getId());
                    } else {
                        // If no MongoDB equivalent, keep H2 ID but log it
                        log.warn("No MongoDB equivalent found for H2 user: {} (ID: {})", h2User.getUsername(), h2User.getId());
                    }
                } catch (Exception e) {
                    log.error("Error mapping user {}: {}", h2User.getUsername(), e.getMessage());
                }
            }

            // Step 2: Update all predictions to use correct userIds
            java.util.List<com.ipl.model.mongo.UserPrediction> allPredictions =
                userPredictionRepository.findAll();

            int updatedCount = 0;
            for (com.ipl.model.mongo.UserPrediction prediction : allPredictions) {
                Long currentUserId = prediction.getUserId();
                Long correctUserId = userIdMapping.get(currentUserId);

                if (correctUserId != null && !correctUserId.equals(currentUserId)) {
                    log.info("Updating prediction {}: userId {} -> {}", prediction.getId(), currentUserId, correctUserId);
                    prediction.setUserId(correctUserId);
                    userPredictionRepository.save(prediction);
                    updatedCount++;
                }
            }

            // Step 3: Remove duplicate predictions (same user + match)
            removeDuplicatePredictions();

            log.info("User identity consolidation completed:");
            log.info("  - {} predictions updated with correct userIds", updatedCount);
            log.info("  - Duplicate predictions removed");

        } catch (Exception e) {
            log.error("Error consolidating user identities: {}", e.getMessage());
        }
    }

    /**
     * Remove duplicate predictions for same user + match combination
     */
    private void removeDuplicatePredictions() {
        try {
            java.util.List<com.ipl.model.mongo.UserPrediction> allPredictions =
                userPredictionRepository.findAll();

            // Group by userId + matchId
            java.util.Map<String, java.util.List<com.ipl.model.mongo.UserPrediction>> groupedPredictions =
                new java.util.HashMap<>();

            for (com.ipl.model.mongo.UserPrediction pred : allPredictions) {
                String key = pred.getUserId() + "_" + pred.getMatchId();
                groupedPredictions.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(pred);
            }

            int duplicatesRemoved = 0;
            for (java.util.Map.Entry<String, java.util.List<com.ipl.model.mongo.UserPrediction>> entry : groupedPredictions.entrySet()) {
                java.util.List<com.ipl.model.mongo.UserPrediction> predictions = entry.getValue();
                if (predictions.size() > 1) {
                    // Keep the most recent prediction, remove others
                    predictions.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    for (int i = 1; i < predictions.size(); i++) {
                        log.info("Removing duplicate prediction: userId={}, matchId={}, createdAt={}",
                            predictions.get(i).getUserId(), predictions.get(i).getMatchId(), predictions.get(i).getCreatedAt());
                        userPredictionRepository.delete(predictions.get(i));
                        duplicatesRemoved++;
                    }
                }
            }

            log.info("Removed {} duplicate predictions", duplicatesRemoved);

        } catch (Exception e) {
            log.error("Error removing duplicate predictions: {}", e.getMessage());
        }
    }

    /**
     * Get the correct userId for a given username (prioritizes MongoDB)
     */
    public Long getCanonicalUserId(String username) {
        try {
            // First try MongoDB
            Optional<UserMongo> mongoUser = userMongoRepository.findByUsername(username);
            if (mongoUser.isPresent()) {
                return mongoUser.get().getId();
            }

            // Fallback to H2, but convert to MongoDB ID if possible
            Optional<User> h2User = userRepository.findByUsername(username);
            if (h2User.isPresent()) {
                User user = h2User.get();
                // Check if we have a MongoDB equivalent
                Optional<UserMongo> mongoEquivalent = userMongoRepository.findByUsername(username);
                if (mongoEquivalent.isPresent()) {
                    return mongoEquivalent.get().getId();
                }
                // If no MongoDB equivalent, return H2 ID (but this shouldn't happen for active users)
                return user.getId();
            }
        } catch (Exception e) {
            log.error("Error getting canonical userId for {}: {}", username, e.getMessage());
        }

        return null;
    }

    /**
     * Validate that a userId exists in our user databases
     */
    public boolean isValidUserId(Long userId) {
        if (userId == null) return false;

        // Check MongoDB first (canonical source)
        try {
            if (userMongoRepository.existsById(userId)) {
                return true;
            }
        } catch (Exception e) {
            log.warn("MongoDB userId validation failed for {}: {}", userId, e.getMessage());
        }

        // Check H2 as fallback
        try {
            if (userRepository.existsById(userId)) {
                return true;
            }
        } catch (Exception e) {
            log.warn("H2 userId validation failed for {}: {}", userId, e.getMessage());
        }

        return false;
    }

    /**
     * Reset password for a user by email (without requiring current password)
     * Admin only functionality
     */
    public void resetPassword(String email, String newPassword) {
        // Find user directly in MongoDB
        UserMongo userMongo = userMongoRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Update password without verifying current password
        String encodedPassword = passwordEncoder.encode(newPassword);
        userMongo.setPassword(encodedPassword);
        userMongo.setUpdatedAt(System.currentTimeMillis());
        userMongoRepository.save(userMongo);

        log.info("Password reset successfully for user: {} (MongoDB only)", userMongo.getUsername());
    }

    /**
     * Change password for a user by email
     */
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        // Find user directly in MongoDB
        UserMongo userMongo = userMongoRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, userMongo.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Update password only in MongoDB
        String encodedPassword = passwordEncoder.encode(newPassword);
        userMongo.setPassword(encodedPassword);
        userMongo.setUpdatedAt(System.currentTimeMillis());
        userMongoRepository.save(userMongo);

        log.info("Password changed successfully for user: {} (MongoDB only)", userMongo.getUsername());
    }

    /**
     * Validate and clean up orphaned predictions (predictions with invalid userIds)
     */
    public void cleanupOrphanedPredictions() {
        try {
            log.info("=== CLEANING UP ORPHANED PREDICTIONS ===");

            java.util.List<com.ipl.model.mongo.UserPrediction> allPredictions =
                userPredictionRepository.findAll();

            int orphanedCount = 0;
            java.util.List<String> orphanedIds = new java.util.ArrayList<>();

            for (com.ipl.model.mongo.UserPrediction prediction : allPredictions) {
                if (!isValidUserId(prediction.getUserId())) {
                    log.warn("Found orphaned prediction: userId={}, matchId={}, username={}",
                        prediction.getUserId(), prediction.getMatchId(), prediction.getUsername());
                    orphanedIds.add(prediction.getId());
                    orphanedCount++;
                }
            }

            // Remove orphaned predictions
            for (String predictionId : orphanedIds) {
                try {
                    userPredictionRepository.deleteById(predictionId);
                    log.info("Removed orphaned prediction: {}", predictionId);
                } catch (Exception e) {
                    log.error("Failed to remove orphaned prediction {}: {}", predictionId, e.getMessage());
                }
            }

            log.info("Cleanup completed: {} orphaned predictions removed", orphanedCount);

        } catch (Exception e) {
            log.error("Error cleaning up orphaned predictions: {}", e.getMessage());
        }
    }
}