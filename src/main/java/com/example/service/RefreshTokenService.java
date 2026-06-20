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
    public RefreshToken createRefreshToken(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 1. Si el usuario ya tenía un token anterior, lo borramos (para obligarlo a usar el nuevo)
        refreshTokenRepository.deleteByUser(user);

        // 2. Creamos un nuevo token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString()); // Genera un código aleatorio estilo "123e4567-e89b-12d3-a456-426614174000"

        // 3. Lo guardamos en Postgres
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
