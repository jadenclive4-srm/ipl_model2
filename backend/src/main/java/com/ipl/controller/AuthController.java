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
    public ResponseEntity<AuthDTO> register(@RequestBody AuthDTO authDTO) {
        User user = userService.registerUser(
                authDTO.getUsername(),
                authDTO.getUniqueUserId(),
                authDTO.getEmail(),
                authDTO.getPassword(),
                authDTO.getFullName(),
                authDTO.getRole()
        );
        
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
    
    @PostMapping("/login")
    public ResponseEntity<AuthDTO> login(@RequestBody AuthDTO authDTO) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authDTO.getUsername(),
                            authDTO.getPassword()
                    )
            );
            
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
            System.out.println("LOGIN FAILED: " + e.getMessage());
            return ResponseEntity.status(401).build();
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