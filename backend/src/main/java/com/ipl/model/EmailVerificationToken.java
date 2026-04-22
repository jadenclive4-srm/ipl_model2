package com.ipl.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "email_verification_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Long expiresAt;

    @Column(nullable = false)
    private Boolean used = false;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now().toEpochMilli();
    }
}
