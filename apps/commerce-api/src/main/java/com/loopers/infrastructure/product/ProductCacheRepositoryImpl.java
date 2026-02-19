package com.loopers.infrastructure.product;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.brand.BrandId;
import com.loopers.domain.product.ProductCacheRepository;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.product.ProductInfo;
import com.loopers.support.cache.CacheablePage;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductCacheRepositoryImpl implements ProductCacheRepository {

	private static final Duration PRODUCT_LIST_TTL = Duration.ofMinutes(30);
	private static final Duration PRODUCT_DETAIL_TTL = Duration.ofMinutes(60);
	private static final long PRODUCT_LIST_TTL_SECONDS = PRODUCT_LIST_TTL.toSeconds();
	private static final long PRODUCT_DETAIL_TTL_SECONDS = PRODUCT_DETAIL_TTL.toSeconds();
	private static final long PRODUCT_LIST_TTL_JITTER_SECONDS = PRODUCT_LIST_TTL_SECONDS / 10;
	private static final long PRODUCT_DETAIL_TTL_JITTER_SECONDS = PRODUCT_DETAIL_TTL_SECONDS / 10;
	private static final String PRODUCT_LIST_EVICT_FAILURE_MESSAGE = "상품 리스트 캐시 삭제 중 오류가 발생했습니다.";
	private static final TypeReference<CacheablePage<ProductInfo>> PRODUCT_INFO_PAGE_TYPE = new TypeReference<>() {
	};

	private final RedisTemplate<String, Object> redisTemplate;
	private final ObjectMapper objectMapper;

	@Override
	public Optional<CacheablePage<ProductInfo>> findProductList(final BrandId brandId, final Pageable pageable) {
		String cacheKey = ProductCacheKeys.buildProductListKey(brandId, pageable);
		Object cached = redisTemplate.opsForValue().get(cacheKey);
		if (cached == null) {
			return Optional.empty();
		}
		return Optional.of(objectMapper.convertValue(cached, PRODUCT_INFO_PAGE_TYPE));
	}

	@Override
	public void saveProductList(final BrandId brandId, final Pageable pageable, final CacheablePage<ProductInfo> productList) {
		String cacheKey = ProductCacheKeys.buildProductListKey(brandId, pageable);
		redisTemplate.opsForValue().set(cacheKey, productList,
			generateTtlWithJitter(PRODUCT_LIST_TTL_SECONDS, PRODUCT_LIST_TTL_JITTER_SECONDS));
	}

	@Override
	public Optional<ProductInfo> findProductDetail(final ProductId productId) {
		String cacheKey = ProductCacheKeys.buildProductDetailKey(productId);
		Object cached = redisTemplate.opsForValue().get(cacheKey);
		if (cached instanceof ProductInfo productInfo) {
			return Optional.of(productInfo);
		}
		return Optional.empty();
	}

	@Override
	public void saveProductDetail(final ProductId productId, final ProductInfo productInfo) {
		String cacheKey = ProductCacheKeys.buildProductDetailKey(productId);
		redisTemplate.opsForValue().set(cacheKey, productInfo,
			generateTtlWithJitter(PRODUCT_DETAIL_TTL_SECONDS, PRODUCT_DETAIL_TTL_JITTER_SECONDS));
	}

	@Override
	public Map<ProductId, ProductInfo> findProductDetailsByIds(final List<ProductId> productIds) {
		if (productIds == null || productIds.isEmpty()) {
			return Map.of();
		}

		List<String> cacheKeys = productIds.stream().map(ProductCacheKeys::buildProductDetailKey).toList();
		List<Object> cachedValues = redisTemplate.opsForValue().multiGet(cacheKeys);
		if (cachedValues == null || cachedValues.isEmpty()) {
			return Map.of();
		}

		Map<ProductId, ProductInfo> cachedProducts = new HashMap<>();
		for (int i = 0; i < productIds.size(); i++) {
			Object value = cachedValues.get(i);
			if (value instanceof ProductInfo productInfo) {
				cachedProducts.put(productIds.get(i), productInfo);
			}
		}
		return cachedProducts;
	}

	@Override
	public void saveProductDetails(final List<ProductInfo> productInfos) {
		if (productInfos == null || productInfos.isEmpty()) {
			return;
		}

		for (ProductInfo productInfo : productInfos) {
			saveProductDetail(ProductId.of(productInfo.productId()), productInfo);
		}
	}

	@Override
	public void evictProductDetail(final ProductId productId) {
		redisTemplate.delete(ProductCacheKeys.buildProductDetailKey(productId));
	}

	@Override
	public void evictProductList() {
		RedisScanBatchEvictor.evictByPattern(
			redisTemplate,
			ProductCacheKeys.buildProductListPattern(),
			PRODUCT_LIST_EVICT_FAILURE_MESSAGE
		);
	}

	private Duration generateTtlWithJitter(final long baseSeconds, final long jitterRangeSeconds) {
		long jitterOffsetSeconds = ThreadLocalRandom.current().nextLong(-jitterRangeSeconds, jitterRangeSeconds + 1);
		long ttlSeconds = Math.max(1, baseSeconds + jitterOffsetSeconds);
		return Duration.ofSeconds(ttlSeconds);
	}
}
