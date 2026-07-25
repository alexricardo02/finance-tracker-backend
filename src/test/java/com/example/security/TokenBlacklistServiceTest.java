package com.example.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.example.security.TokenBlacklistService;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks private TokenBlacklistService tokenBlacklistService;

    @Test
    void blacklist_validJti_setsKeyWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenBlacklistService.blacklist("abc-123", 900);

        verify(valueOperations).set(eq("blacklist:jti:abc-123"), eq("1"), eq(Duration.ofSeconds(900)));
    }

    @Test
    void blacklist_nullJti_doesNothing() {
        tokenBlacklistService.blacklist(null, 900);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void blacklist_nonPositiveTtl_doesNothing() {
        tokenBlacklistService.blacklist("abc-123", 0);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void isBlacklisted_keyPresent_returnsTrue() {
        when(redisTemplate.hasKey("blacklist:jti:abc-123")).thenReturn(true);

        assertThat(tokenBlacklistService.isBlacklisted("abc-123")).isTrue();
    }

    @Test
    void isBlacklisted_keyAbsent_returnsFalse() {
        when(redisTemplate.hasKey("blacklist:jti:abc-123")).thenReturn(false);

        assertThat(tokenBlacklistService.isBlacklisted("abc-123")).isFalse();
    }

    @Test
    void isBlacklisted_nullJti_returnsFalse_noRedisCall() {
        assertThat(tokenBlacklistService.isBlacklisted(null)).isFalse();

        verifyNoInteractions(redisTemplate);
    }
}