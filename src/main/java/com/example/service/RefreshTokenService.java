package com.example.service;

import com.example.models.RefreshToken;
import com.example.models.User;
import com.example.repository.RefreshTokenRepository;
import com.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

	@Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    // Duración del Refresh Token: 7 días (en milisegundos)
    private final Long refreshTokenDurationMs = 604800000L;
    
    @Transactional
    public RefreshToken createRefreshToken(int userId) {
        RefreshToken refreshToken = refreshTokenRepository.findByUserId(userId)
                .orElse(new RefreshToken());

        refreshToken.setUser(userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found")));
        refreshToken.setToken(java.util.UUID.randomUUID().toString());
        refreshToken.setExpiryDate(java.time.Instant.now().plusMillis(refreshTokenDurationMs)); // <-- antes: 86400000L hardcodeado

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        // Si la fecha actual es MAYOR que la fecha de expiración del token...
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token); // Lo borramos por inútil
            throw new SecurityException("El Refresh Token ha expirado. Por favor, inicia sesión de nuevo.");
        }
        return token;
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
}
