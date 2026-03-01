package com.loopers.domain.brand;

import org.springframework.stereotype.Component;

import com.loopers.support.metrics.CacheMetricsCore;

@Component
public class BrandCacheMetrics {

	private static final String METRIC_DOMAIN = "brand";

	private final CacheMetricsCore cacheMetricsCore;

	public BrandCacheMetrics(final CacheMetricsCore cacheMetricsCore) {
		this.cacheMetricsCore = cacheMetricsCore;
	}

	public void recordCacheHit() {
		cacheMetricsCore.recordCacheHit(METRIC_DOMAIN);
	}

	public void recordCacheMiss() {
		cacheMetricsCore.recordCacheMiss(METRIC_DOMAIN);
	}

	public void recordDbLoad() {
		cacheMetricsCore.recordDbLoad(METRIC_DOMAIN);
	}
}
