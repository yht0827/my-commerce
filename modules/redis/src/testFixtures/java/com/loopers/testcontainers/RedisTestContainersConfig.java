package com.loopers.testcontainers;

import org.springframework.context.annotation.Configuration;
import org.testcontainers.utility.DockerImageName;

import com.redis.testcontainers.RedisContainer;

@Configuration
public class RedisTestContainersConfig {
	private static final String REDIS_IMAGE = "redis:7.0";

	private static final RedisContainer redisContainer = new RedisContainer(DockerImageName.parse(REDIS_IMAGE));

	static {
		redisContainer.start();
		String host = redisContainer.getHost();
		String port = String.valueOf(redisContainer.getFirstMappedPort());

		System.setProperty("REDIS_MASTER_HOST", host);
		System.setProperty("REDIS_MASTER_PORT", port);
		System.setProperty("REDIS_REPLICA_1_HOST", host);
		System.setProperty("REDIS_REPLICA_1_PORT", port);
	}
}
