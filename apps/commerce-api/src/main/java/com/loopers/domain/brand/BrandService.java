package com.loopers.domain.brand;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class BrandService {

	private static final Duration L2_BRAND_TTL = Duration.ofHours(2);
	private static final String CACHE_KEY_PREFIX_BRAND = "brand";

	private final BrandRepository brandRepository;
	private final Cache<String, Object> brandL1Cache;
	private final RedisTemplate<String, Object> brandL2Cache;

	public BrandInfo getBrandById(final BrandId brandId) {
		String cacheKey = CACHE_KEY_PREFIX_BRAND + ":" + brandId.getBrandId();

		// 1. L1 캐시 확인 (Cache-Aside)
		BrandInfo l1Result = (BrandInfo)brandL1Cache.getIfPresent(cacheKey);
		if (l1Result != null) {
			log.debug("L1 Cache hit: {}", cacheKey);
			return l1Result;
		}

		// 2. L2 캐시 확인
		BrandInfo l2Result = (BrandInfo)brandL2Cache.opsForValue().get(cacheKey);
		if (l2Result != null) {
			log.debug("L2 Cache hit: {}", cacheKey);
			// L1에 다시 저장 (Write-Through)
			brandL1Cache.put(cacheKey, l2Result);
			return l2Result;
		}

		// 3. 캐시 미스 - DB 조회 및 캐시 저장
		log.debug("Cache miss: {}", cacheKey);
		Brand brand = brandRepository.findById(brandId)
			.orElseThrow(
				() -> new CoreException(ErrorType.NOT_FOUND, "해당 [id = " + brandId.getBrandId() + "]의 브랜드를 찾을 수 없습니다."));

		BrandInfo result = BrandInfo.from(brand);

		// 캐시 저장 (Write-Through)
		brandL2Cache.opsForValue().set(cacheKey, result, L2_BRAND_TTL);
		brandL1Cache.put(cacheKey, result);

		return result;
	}
}
