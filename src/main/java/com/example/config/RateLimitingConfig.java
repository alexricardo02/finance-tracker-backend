package com.example.config;



import java.time.Duration;

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

    @Value("${spring.data.redis.password}")
    private String redisPassword;
    
    @Bean
    public RedisClient redisClient() {
        // redis url para Upstash
        String redisUri = String.format("rediss://default:%s@%s:%d", redisPassword, redisHost, redisPort);
        return RedisClient.create(redisUri);
    }
    
    @Bean
    public ProxyManager<byte[]> proxyManager(RedisClient redisClient) {
        // Configuramos el ProxyManager de la forma más básica y segura
        return LettuceBasedProxyManager.builderFor(redisClient)
                .build();
    }
	
}
