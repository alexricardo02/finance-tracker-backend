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
        org.springframework.data.redis.connection.RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
                 try (Cursor<byte[]> cursor = connection.scan(options)) {
                     while (cursor.hasNext()) {
                         connection.del(cursor.next());
                     }
                 } finally {
                     connection.close(); // Retorna la conexión al pool para evitar fugas de memoria
                 }
    }
    
 // WHY: Explicitly target global dictionary keys that don't contain a username
    public void evictGlobalCache(String cacheName, String key) {
        redisTemplate.delete(cacheName + "::" + key);
    }
    
}