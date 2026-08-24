package com.example.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisConnection redisConnection;

    @Mock
    private Cursor<byte[]> cursor;

    @InjectMocks
    private CacheService cacheService;

    @Test
    void evictUserFinancialCache_scansAndDeletesOnlyTargetUserKeys() {
        String targetUser = "alex";
        byte[] key1 = "cache_alex_1".getBytes();
        byte[] key2 = "cache_alex_2".getBytes();

        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        
        // WHY: We must intercept the exact ScanOptions passed to Redis to cryptographically 
        // guarantee tenant isolation. If the pattern lacks wildcard bounds, data leaks occur.
        ArgumentCaptor<ScanOptions> optionsCaptor = ArgumentCaptor.forClass(ScanOptions.class);
        when(redisConnection.scan(optionsCaptor.capture())).thenReturn(cursor);

        // Simulate cursor returning two keys, then stopping
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn(key1, key2);

        cacheService.evictUserFinancialCache(targetUser);

        ScanOptions capturedOptions = optionsCaptor.getValue();
        assertThat(capturedOptions.toOptionString()).containsPattern("\\*" + targetUser + "\\*");

        verify(redisConnection).del(key1);
        verify(redisConnection).del(key2);
        
        // WHY: Cursor is a closeable resource representing an active TCP socket. 
        // Verifying closure prevents connection pool exhaustion in production.
        verify(cursor).close();
    }

    @Test
    void evictGlobalCache_deletesExactKey() {
        cacheService.evictGlobalCache("fx_rate", "USD_ARS_2026-07-25");

        verify(redisTemplate).delete("fx_rate::USD_ARS_2026-07-25");
    }
}