package com.loopers.config.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		cacheManager.setCaffeine(Caffeine.newBuilder()
			.initialCapacity(100)
			.maximumSize(1000)
			.expireAfterWrite(60, TimeUnit.MINUTES)
			.recordStats());
		return cacheManager;
	}

	@Bean("productL1Cache")
	public Cache<String, Object> productL1Cache() {
		return Caffeine.newBuilder()
			.initialCapacity(100)
			.maximumSize(1000)
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.recordStats()
			.build();
	}

}
