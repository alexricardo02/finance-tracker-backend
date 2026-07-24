package com.example.security;

import io.jsonwebtoken.Claims;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
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
	
  // Secret key (in production it should come from an environment variable)
	@Value("${jwt.secret}")
	private String secret;

	private Key key;

	@PostConstruct
	public void init() {
	    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
	    this.key = Keys.hmacShaKeyFor(keyBytes);
	}
    private final long expirationTime = 900000; // 15 minutes
    
    // Generate token
    public String generateToken(String username, Role role) {
        String jti = UUID.randomUUID().toString();
        return Jwts.builder()
                .setId(jti)
                .setSubject(username)
                .claim("role", role.name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key)
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
