package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Service
public class CacheService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    // WHY: Programmatic eviction allows pattern-matching, preventing global cache wipes (allEntries=true)
    // and ensuring tenant isolation in a shared Redis instance.
    // Financial cache keys look like "incomes_month::alice_JULY". The pattern is anchored to that shape so
    // it cannot reach rate-limit buckets or the JWT blacklist in the same Redis. Usernames are unrestricted,
    // so "alice" also matches "alice_x": that only costs alice_x a cache miss, never wrong data.
    public void evictUserFinancialCache(String username) {
        ScanOptions options = ScanOptions.scanOptions().match("*::" + escapeGlob(username) + "_*").count(100).build();
        org.springframework.data.redis.connection.RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
                 try (Cursor<byte[]> cursor = connection.scan(options)) {
                     while (cursor.hasNext()) {
                         byte[] key = cursor.next();
                         if (isFinancialCacheKey(key)) {
                             connection.del(key);
                         }
                     }
                 } finally {
                     connection.close(); // Retorna la conexión al pool para evitar fugas de memoria
                 }
    }
    
    private static String escapeGlob(String value) {
        return value.replaceAll("([*?\\[\\]\\\\])", "\\\\$1");
    }

    private static boolean isFinancialCacheKey(byte[] key) {
        String name = new String(key, StandardCharsets.UTF_8);
        return name.startsWith("incomes_") || name.startsWith("expenses_");
    }

 // WHY: Explicitly target global dictionary keys that don't contain a username
    public void evictGlobalCache(String cacheName, String key) {
        redisTemplate.delete(cacheName + "::" + key);
    }
    
}