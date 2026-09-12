package com.example.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.models.Role;

import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    // Minimum 32 bytes = 256 bits required for HMAC-SHA-256
    private static final int MIN_KEY_BYTES = 32;

    // Secret key injected from environment variable — never hardcode
    @Value("${jwt.secret}")
    private String secret;

    private Key key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // H-2 fix: enforce minimum secret length before the key is built.
        // Keys.hmacShaKeyFor() would throw on < 32 bytes, but we do it explicitly
        // here so the startup failure is a clear, actionable error message rather than
        // a cryptic WeakKeyException buried in the stack.
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                "JWT secret is too short: " + keyBytes.length + " bytes. " +
                "A minimum of " + MIN_KEY_BYTES + " bytes (256 bits) is required for HS256."
            );
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT signing key initialised ({} bytes)", keyBytes.length);
    }
    
    private final long expirationTime = 900000; // 15 minutes
    
    // Generate token — algorithm explicitly pinned to HS256 (H-2 fix)
    public String generateToken(String username, Role role) {
        String jti = UUID.randomUUID().toString();
        return Jwts.builder()
                .setId(jti)
                .setSubject(username)
                .claim("role", role.name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key, SignatureAlgorithm.HS256) // algorithm pinned — prevents "none" attacks
                .compact();
    }
    
	 // Extract username
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    // Validate token
    public boolean isTokenValid(String token) {
        try {
            return getClaims(token).getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    public String extractRole(String token) {
        Object role = getClaims(token).get("role");
        return role != null ? role.toString() : Role.USER.name();
    }
    

    public String extractJti(String token) {
        return getClaims(token).getId();
    }

    public long getRemainingValiditySeconds(String token) {
        long diff = getClaims(token).getExpiration().getTime() - System.currentTimeMillis();
        return Math.max(diff / 1000, 0);
    }

    public long getExpirationSeconds() {
        return expirationTime / 1000;
    }

}
