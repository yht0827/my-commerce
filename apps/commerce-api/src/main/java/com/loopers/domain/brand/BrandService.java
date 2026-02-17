package com.loopers.domain.brand;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class BrandService {

	private final BrandRepository brandRepository;
	private final BrandCacheRepository brandCacheRepository;
	private final BrandCacheMetrics brandCacheMetrics;

	public BrandInfo getBrandById(final BrandId brandId) {
		Optional<BrandInfo> cached = brandCacheRepository.findById(brandId);
		if (cached.isPresent()) {
			brandCacheMetrics.recordCacheHit();
			log.debug("Redis cache hit: brandId={}", brandId.getBrandId());
			return cached.get();
		}

		brandCacheMetrics.recordCacheMiss();
		return loadFromDatabaseAndCache(brandId);
	}

	private BrandInfo loadFromDatabaseAndCache(final BrandId brandId) {
		brandCacheMetrics.recordDbLoad();
		log.debug("Cache miss: brandId={}", brandId.getBrandId());

		Brand brand = brandRepository.findById(brandId)
			.orElseThrow(
				() -> new CoreException(ErrorType.NOT_FOUND, "해당 [id = " + brandId.getBrandId() + "]의 브랜드를 찾을 수 없습니다."));

		BrandInfo brandInfo = BrandInfo.from(brand);
		brandCacheRepository.save(brandId, brandInfo);
		return brandInfo;
	}
}
