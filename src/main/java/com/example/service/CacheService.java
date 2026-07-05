package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;

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
        // WHY: The wildcard '*' matches all cache namespaces (expenses_*, incomes_*) tied to this specific user.
        String pattern = "*" + username + "*";
        Set<String> keys = redisTemplate.keys(pattern);
        
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
    
 // WHY: Explicitly target global dictionary keys that don't contain a username
    public void evictGlobalCache(String cacheName, String key) {
        redisTemplate.delete(cacheName + "::" + key);
    }
    
}