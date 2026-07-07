package com.example.service;

import com.example.models.PasswordResetToken;
import com.example.models.User;
import com.example.repository.PasswordResetTokenRepository;
import com.example.repository.RefreshTokenRepository;
import com.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final long TOKEN_VALIDITY_MINUTES = 30;

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresentOrElse(user -> {
            passwordResetTokenRepository.deleteByUser(user);

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setExpiryDate(Instant.now().plusSeconds(TOKEN_VALIDITY_MINUTES * 60));
            passwordResetTokenRepository.save(resetToken);

            emailService.sendPasswordResetEmail(user.getEmail(), resetToken.getToken());
        }, () -> log.info("Password reset requested for a non-existent email"));
        // WHY: no exception/branching on "not found" — avoids leaking which emails are registered
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new SecurityException("Invalid or expired reset token"));

        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new SecurityException("Invalid or expired reset token");
        }

        User user = resetToken.getUser();
        user.setPassword_hash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
        // WHY: invalidate existing sessions so a leaked/old session can't survive a password reset
        refreshTokenRepository.deleteByUser(user);
    }
}