package com.loopers.infrastructure.brand;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.loopers.domain.brand.BrandCacheRepository;
import com.loopers.domain.brand.BrandId;
import com.loopers.domain.brand.BrandInfo;
import com.loopers.support.config.CacheVersionProperties;

@Repository
public class BrandCacheRepositoryImpl implements BrandCacheRepository {

	private static final Duration BRAND_TTL = Duration.ofHours(2);
	private static final long BRAND_TTL_SECONDS = BRAND_TTL.toSeconds();
	private static final long BRAND_TTL_JITTER_SECONDS = BRAND_TTL_SECONDS / 10;
	private static final String CACHE_KEY_PREFIX_BRAND = "brand";
	private static final String SEPARATOR = ":";

	private final RedisTemplate<String, Object> redisTemplate;
	private final String brandVersion;

	public BrandCacheRepositoryImpl(
		final RedisTemplate<String, Object> redisTemplate,
		final CacheVersionProperties cacheVersionProperties
	) {
		this.redisTemplate = redisTemplate;
		this.brandVersion = cacheVersionProperties.brand();
	}

	@Override
	public Optional<BrandInfo> findById(final BrandId brandId) {
		String cacheKey = createCacheKey(brandId);
		BrandInfo cached = (BrandInfo)redisTemplate.opsForValue().get(cacheKey);
		return Optional.ofNullable(cached);
	}

	@Override
	public void save(final BrandId brandId, final BrandInfo brandInfo) {
		String cacheKey = createCacheKey(brandId);
		redisTemplate.opsForValue().set(cacheKey, brandInfo, generateTtlWithJitter());
	}

	private String createCacheKey(final BrandId brandId) {
		return brandVersion + SEPARATOR + CACHE_KEY_PREFIX_BRAND + SEPARATOR + brandId.getBrandId();
	}

	private Duration generateTtlWithJitter() {
		long jitterOffsetSeconds = ThreadLocalRandom.current()
			.nextLong(-BRAND_TTL_JITTER_SECONDS, BRAND_TTL_JITTER_SECONDS + 1);
		long ttlSeconds = Math.max(1, BRAND_TTL_SECONDS + jitterOffsetSeconds);
		return Duration.ofSeconds(ttlSeconds);
	}
}
