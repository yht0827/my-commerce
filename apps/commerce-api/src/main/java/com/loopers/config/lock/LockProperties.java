package com.loopers.config.lock;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.lock")
public record LockProperties(
	LockMode mode,
	int optimisticRetryCount
) {
	public LockProperties {
		if (mode == null) mode = LockMode.PESSIMISTIC;
		if (optimisticRetryCount <= 0) optimisticRetryCount = 3;
	}
}
