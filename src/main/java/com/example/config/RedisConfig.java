package com.example.config;

import java.time.Duration;


import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RedisConfig {
	
	
	@Bean
	public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {

	    ObjectMapper redisMapper = new ObjectMapper();
	    redisMapper.registerModule(new JavaTimeModule());
	    redisMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	    RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
	            .disableCachingNullValues()
	            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(redisMapper)))
	            .entryTtl(Duration.ofHours(1));

	    return RedisCacheManager.builder(redisConnectionFactory)
	            .cacheDefaults(cacheConfig)
	            .build();
	}
}
