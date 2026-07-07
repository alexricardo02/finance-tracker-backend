package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class CacheService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    // WHY: Programmatic eviction allows pattern-matching, preventing global cache wipes (allEntries=true)
    // and ensuring tenant isolation in a shared Redis instance.
    public void evictUserFinancialCache(String username) {
        ScanOptions options = ScanOptions.scanOptions().match("*" + username + "*").count(100).build();
        try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory().getConnection().scan(options)) {
            cursor.forEachRemaining(key -> redisTemplate.getConnectionFactory().getConnection().del(key));
        }
    }
    
 // WHY: Explicitly target global dictionary keys that don't contain a username
    public void evictGlobalCache(String cacheName, String key) {
        redisTemplate.delete(cacheName + "::" + key);
    }
    
}