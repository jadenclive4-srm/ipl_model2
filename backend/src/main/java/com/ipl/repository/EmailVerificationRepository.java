package com.ipl.repository;

import com.ipl.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByEmailAndTokenHashAndUsedFalse(String email, String tokenHash);

    Optional<EmailVerificationToken> findByEmailAndUsedFalse(String email);

    void deleteByEmail(String email);
}
