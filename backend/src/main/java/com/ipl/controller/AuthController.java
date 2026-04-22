package com.ipl.controller;

import com.ipl.dto.AuthDTO;
import com.ipl.model.User;
import com.ipl.service.UserService;
import com.ipl.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody AuthDTO authDTO) {
        try {
            User user = userService.registerUser(
                    authDTO.getUsername(),
                    authDTO.getUniqueUserId(),
                    authDTO.getEmail(),
                    authDTO.getPassword(),
                    authDTO.getFullName(),
                    authDTO.getRole()
            );
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "OTP_SENT");
            response.put("email", user.getEmail());
            response.put("username", user.getUsername());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            System.err.println("REGISTRATION ERROR: " + e.getMessage());
            throw e; // Re-throw for GlobalExceptionHandler
        }
    }
    
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthDTO> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        
        if (email == null || otp == null) {
            return ResponseEntity.badRequest().build();
        }
        
        User user = userService.verifyOtp(email, otp);
        
        String token = jwtUtil.generateToken(user.getUsername());
        
        AuthDTO response = new AuthDTO();
        response.setUsername(user.getUsername());
        response.setUniqueUserId(user.getUniqueUserId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setToken(token);
        response.setUserId(user.getId());
        response.setRole(user.getRole());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, String>> resendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        userService.resendVerificationOtp(email);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "OTP_RESENT");
        response.put("email", email);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthDTO authDTO) {
        System.out.println("LOGIN attempt for username: " + authDTO.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authDTO.getUsername(),
                            authDTO.getPassword()
                    )
            );
            System.out.println("Authentication successful for: " + authDTO.getUsername());
            
            User user = userService.findByUsername(authDTO.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            String token = jwtUtil.generateToken(user.getUsername());
            
            AuthDTO response = new AuthDTO();
            response.setUsername(user.getUsername());
            response.setEmail(user.getEmail());
            response.setFullName(user.getFullName());
            response.setToken(token);
            response.setUserId(user.getId());
            response.setRole(user.getRole());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("LOGIN FAILED: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid username or password");
            return ResponseEntity.status(401).body(error);
        }
    }
    
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Boolean>> validateToken(@RequestHeader("Authorization") String authHeader) {
        Map<String, Boolean> response = new HashMap<>();
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);
            boolean isValid = jwtUtil.validateToken(token, username);
            response.put("valid", isValid);
        } else {
            response.put("valid", false);
        }
        
        return ResponseEntity.ok(response);
    }
}