package com.example.service;

import com.example.models.RefreshToken;
import com.example.models.User;
import com.example.repository.RefreshTokenRepository;
import com.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("john", "john@test.com", "hash", new Date());
        user.setUser_id(1);
    }

    @Test
    void createRefreshToken_noExistingToken_createsNewOne() {
        when(refreshTokenRepository.findByUserId(1)).thenReturn(Optional.empty());
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(1);

        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getToken()).isNotBlank();
        assertThat(result.getExpiryDate()).isAfter(Instant.now());
    }

    @Test
    void createRefreshToken_existingToken_rotatesTheSameRecord_singleTokenPerUser() {
        // WHY: current design is single-token-per-user (see roadmap item: multi-device sessions).
        RefreshToken existing = new RefreshToken();
        existing.setId(5);
        existing.setToken("old-token");

        when(refreshTokenRepository.findByUserId(1)).thenReturn(Optional.of(existing));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(1);

        assertThat(result.getId()).isEqualTo(5);
        assertThat(result.getToken()).isNotEqualTo("old-token");
    }

    @Test
    void createRefreshToken_userNotFound_throwsRuntimeException() {
        when(refreshTokenRepository.findByUserId(1)).thenReturn(Optional.empty());
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.createRefreshToken(1))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void verifyExpiration_notExpired_returnsSameToken() {
        RefreshToken token = new RefreshToken();
        token.setExpiryDate(Instant.now().plusSeconds(3600));

        RefreshToken result = refreshTokenService.verifyExpiration(token);

        assertThat(result).isEqualTo(token);
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void verifyExpiration_expired_deletesAndThrowsSecurityException() {
        RefreshToken token = new RefreshToken();
        token.setExpiryDate(Instant.now().minusSeconds(1));

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
                .isInstanceOf(SecurityException.class);

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void findByToken_delegatesToRepository() {
        RefreshToken token = new RefreshToken();
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        assertThat(refreshTokenService.findByToken("abc")).contains(token);
    }

    @Test
    void rotateRefreshToken_generatesNewTokenAndExtendsExpiry() {
        RefreshToken token = new RefreshToken();
        token.setToken("old-token");
        Instant oldExpiry = Instant.now().minusSeconds(10);
        token.setExpiryDate(oldExpiry);

        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = refreshTokenService.rotateRefreshToken(token);

        assertThat(result.getToken()).isNotEqualTo("old-token");
        assertThat(result.getExpiryDate()).isAfter(oldExpiry);
    }

    @Test
    void deleteByToken_delegatesToRepository() {
        refreshTokenService.deleteByToken("abc");

        verify(refreshTokenRepository).deleteByToken("abc");
    }
}