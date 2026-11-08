package com.example.config;

import java.time.Duration;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RedisConfig implements CachingConfigurer {

	private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);
	
	
	@Bean
	public RedisCacheConfiguration cacheConfiguration() {

	    ObjectMapper redisMapper = new ObjectMapper();
	    redisMapper.registerModule(new JavaTimeModule());
	    redisMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	    
	    redisMapper.activateDefaultTyping(
		        redisMapper.getPolymorphicTypeValidator(),
		        ObjectMapper.DefaultTyping.NON_FINAL,
		        JsonTypeInfo.As.PROPERTY
		    );

	    return RedisCacheConfiguration.defaultCacheConfig()
	            .disableCachingNullValues()
	            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(redisMapper)))
	            .entryTtl(Duration.ofHours(1));
	}

	// Entries written before this config loaded are JDK-serialized with no TTL, and Jackson cannot
	// read them. Treating a failed read as a miss replaces them with a readable entry that expires.
	@Override
	public CacheErrorHandler errorHandler() {
		return new SimpleCacheErrorHandler() {
			@Override
			public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
				log.warn("Unreadable entry in cache {}; reloading it", cache.getName(), exception);
			}
		};
	}
}
