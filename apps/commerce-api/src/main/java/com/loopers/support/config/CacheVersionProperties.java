package com.loopers.support.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cache.version")
public record CacheVersionProperties(
	String product,
	String brand
) {

	private static final String DEFAULT_VERSION = "v1";

	public String product() {
		return product != null ? product : DEFAULT_VERSION;
	}

	public String brand() {
		return brand != null ? brand : DEFAULT_VERSION;
	}
}
