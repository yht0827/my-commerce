package com.loopers.support.metrics;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;

@Component
public class CacheMetricsCore {

	private static final String CACHE_REQUEST_METRIC = "commerce.cache.request";
	private static final String CACHE_DB_LOAD_METRIC = "commerce.cache.db.load";
	private static final String CACHE_BATCH_COUNT_METRIC = "commerce.cache.batch.count";
	private static final String TAG_CACHE = "cache";
	private static final String TAG_RESULT = "result";
	private static final String RESULT_HIT = "hit";
	private static final String RESULT_MISS = "miss";

	private final MeterRegistry meterRegistry;

	public CacheMetricsCore(final MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void recordCacheHit(final String domain) {
		increment(CACHE_REQUEST_METRIC, domain, RESULT_HIT);
	}

	public void recordCacheMiss(final String domain) {
		increment(CACHE_REQUEST_METRIC, domain, RESULT_MISS);
	}

	public void recordDbLoad(final String domain) {
		increment(CACHE_DB_LOAD_METRIC, domain);
	}

	public void recordBatchCacheHitCount(final String domain, final int count) {
		if (count <= 0) {
			return;
		}
		increment(CACHE_BATCH_COUNT_METRIC, domain, RESULT_HIT, count);
	}

	public void recordBatchCacheMissCount(final String domain, final int count) {
		if (count <= 0) {
			return;
		}
		increment(CACHE_BATCH_COUNT_METRIC, domain, RESULT_MISS, count);
	}

	private void increment(final String metricName, final String domain) {
		meterRegistry.counter(metricName, TAG_CACHE, domain).increment();
	}

	private void increment(final String metricName, final String domain, final String result) {
		meterRegistry.counter(metricName, TAG_CACHE, domain, TAG_RESULT, result).increment();
	}

	private void increment(final String metricName, final String domain, final String result, final int count) {
		meterRegistry.counter(metricName, TAG_CACHE, domain, TAG_RESULT, result).increment(count);
	}
}
