package com.loopers.domain.product;

import org.springframework.stereotype.Component;

import com.loopers.support.metrics.CacheMetricsCore;

@Component
public class ProductCacheMetrics {

	private static final String METRIC_DOMAIN = "product";

	private final CacheMetricsCore cacheMetricsCore;

	public ProductCacheMetrics(final CacheMetricsCore cacheMetricsCore) {
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

	public void recordBatchCacheHitCount(final int count) {
		cacheMetricsCore.recordBatchCacheHitCount(METRIC_DOMAIN, count);
	}

	public void recordBatchCacheMissCount(final int count) {
		cacheMetricsCore.recordBatchCacheMissCount(METRIC_DOMAIN, count);
	}
}
