package com.example.service;

import com.example.models.PasswordResetToken;
import com.example.models.User;
import com.example.repository.PasswordResetTokenRepository;
import com.example.repository.RefreshTokenRepository;
import com.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks private PasswordResetService passwordResetService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("john", "john@test.com", "hash", new Date());
        user.setUser_id(1);
    }

    @Test
    void requestPasswordReset_existingEmail_createsTokenAndSendsEmail() {
        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));

        passwordResetService.requestPasswordReset("john@test.com");

        verify(passwordResetTokenRepository).deleteByUser(user);

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getToken()).isNotBlank();
        assertThat(captor.getValue().getExpiryDate()).isAfter(Instant.now());

        verify(emailService).sendPasswordResetEmail(eq("john@test.com"), anyString());
    }

    @Test
    void requestPasswordReset_unknownEmail_doesNotLeakExistenceOrThrow() {
        // WHY: security-critical — response/behavior must be identical for existing vs non-existing emails.
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        passwordResetService.requestPasswordReset("ghost@test.com");

        verifyNoInteractions(emailService, passwordResetTokenRepository);
    }

    @Test
    void resetPassword_validToken_updatesPasswordAndInvalidatesSessions() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("valid-token");
        token.setUser(user);
        token.setExpiryDate(Instant.now().plusSeconds(600));

        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newPass123")).thenReturn("newHashed");

        passwordResetService.resetPassword("valid-token", "newPass123");

        assertThat(user.getPassword_hash()).isEqualTo("newHashed");
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).delete(token);
        verify(refreshTokenRepository).deleteByUser(user);
    }

    @Test
    void resetPassword_unknownToken_throwsSecurityException() {
        when(passwordResetTokenRepository.findByToken("bogus")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("bogus", "newPass123"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void resetPassword_expiredToken_deletesTokenAndThrowsSecurityException_doesNotUpdatePassword() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("expired-token");
        token.setUser(user);
        token.setExpiryDate(Instant.now().minusSeconds(10));

        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword("expired-token", "newPass123"))
                .isInstanceOf(SecurityException.class);

        verify(passwordResetTokenRepository).delete(token);
        verify(userRepository, never()).save(any());
    }
}