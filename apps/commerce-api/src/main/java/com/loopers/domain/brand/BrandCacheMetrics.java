package com.loopers.domain.brand;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class BrandCacheMetrics {

	private final Counter cacheHitCounter;
	private final Counter cacheMissCounter;
	private final Counter dbLoadCounter;

	public BrandCacheMetrics(final MeterRegistry meterRegistry) {
		this.cacheHitCounter = meterRegistry.counter("commerce.brand.cache.hit");
		this.cacheMissCounter = meterRegistry.counter("commerce.brand.cache.miss");
		this.dbLoadCounter = meterRegistry.counter("commerce.brand.cache.db.load");
	}

	public void recordCacheHit() {
		cacheHitCounter.increment();
	}

	public void recordCacheMiss() {
		cacheMissCounter.increment();
	}

	public void recordDbLoad() {
		dbLoadCounter.increment();
	}
}
