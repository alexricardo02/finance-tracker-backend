package com.example.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TokenBlacklistService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String PREFIX = "blacklist:jti:";

    public void blacklist(String jti, long ttlSeconds) {
        if (jti == null || ttlSeconds <= 0) return;
        redisTemplate.opsForValue().set(PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null) return false;
        return redisTemplate.hasKey(PREFIX + jti);
    }
}