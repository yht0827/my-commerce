package com.loopers.infrastructure.product;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.loopers.domain.brand.BrandId;
import com.loopers.domain.product.ProductCacheRepository;
import com.loopers.domain.product.ProductInfo;
import com.loopers.domain.product.ProductId;
import com.loopers.support.cache.CacheablePage;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductCacheRepositoryImpl implements ProductCacheRepository {

	private static final Duration PRODUCT_LIST_TTL = Duration.ofMinutes(30);
	private static final Duration PRODUCT_DETAIL_TTL = Duration.ofMinutes(60);
	private static final String CACHE_KEY_PREFIX_PRODUCT_LIST = "productList";
	private static final String CACHE_KEY_PREFIX_PRODUCT_DETAIL = "productDetail";

	private final RedisTemplate<String, Object> redisTemplate;

	@Override
	@SuppressWarnings("unchecked")
	public Optional<CacheablePage<ProductInfo>> findProductList(final BrandId brandId, final Pageable pageable) {
		String cacheKey = productListCacheKey(brandId, pageable);
		CacheablePage<ProductInfo> cached = (CacheablePage<ProductInfo>)redisTemplate.opsForValue().get(cacheKey);
		return Optional.ofNullable(cached);
	}

	@Override
	public void saveProductList(final BrandId brandId, final Pageable pageable, final CacheablePage<ProductInfo> productList) {
		String cacheKey = productListCacheKey(brandId, pageable);
		redisTemplate.opsForValue().set(cacheKey, productList, PRODUCT_LIST_TTL);
	}

	@Override
	public Optional<ProductInfo> findProductDetail(final ProductId productId) {
		String cacheKey = productDetailCacheKey(productId);
		ProductInfo cached = (ProductInfo)redisTemplate.opsForValue().get(cacheKey);
		return Optional.ofNullable(cached);
	}

	@Override
	public void saveProductDetail(final ProductId productId, final ProductInfo productInfo) {
		String cacheKey = productDetailCacheKey(productId);
		redisTemplate.opsForValue().set(cacheKey, productInfo, PRODUCT_DETAIL_TTL);
	}

	@Override
	public Map<ProductId, ProductInfo> findProductDetailsByIds(final List<ProductId> productIds) {
		if (productIds == null || productIds.isEmpty()) {
			return Map.of();
		}

		List<String> cacheKeys = productIds.stream().map(this::productDetailCacheKey).toList();
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
		redisTemplate.delete(productDetailCacheKey(productId));
	}

	@Override
	public void evictProductList() {
		Set<String> keys = redisTemplate.keys(productListCachePattern());
		if (keys == null || keys.isEmpty()) {
			return;
		}
		redisTemplate.delete(keys);
	}

	private String productListCacheKey(final BrandId brandId, final Pageable pageable) {
		Long cacheBrandId = brandId == null ? null : brandId.getBrandId();
		return CACHE_KEY_PREFIX_PRODUCT_LIST + ":" + cacheBrandId + ":" + pageable.getPageNumber() + ":" + pageable.getPageSize();
	}

	private String productListCachePattern() {
		return CACHE_KEY_PREFIX_PRODUCT_LIST + ":*";
	}

	private String productDetailCacheKey(final ProductId productId) {
		return CACHE_KEY_PREFIX_PRODUCT_DETAIL + ":" + productId.getProductId();
	}
}
