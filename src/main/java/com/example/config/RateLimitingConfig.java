package com.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;

@Configuration
public class RateLimitingConfig {
	
	@Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;
    
    @Value("${spring.data.redis.ssl.enabled}")
    private boolean sslEnabled;

    @Bean
    public RedisClient redisClient() {
        String scheme = sslEnabled ? "rediss" : "redis";
        
        String redisUri;
        if (redisPassword != null && !redisPassword.isEmpty()) {
            redisUri = String.format("%s://default:%s@%s:%d", scheme, redisPassword, redisHost, redisPort);
        } else {
            redisUri = String.format("%s://%s:%d", scheme, redisHost, redisPort);
        }
        
        return RedisClient.create(redisUri);
    }
    
    @Bean
    public ProxyManager<byte[]> proxyManager(RedisClient redisClient) {
        return LettuceBasedProxyManager.builderFor(redisClient)
                .build();
    }
	
}
