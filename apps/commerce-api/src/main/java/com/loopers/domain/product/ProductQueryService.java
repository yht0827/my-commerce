package com.loopers.domain.product;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.brand.BrandId;
import com.loopers.support.cache.CacheablePage;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorMessage;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQueryService {

	private static final int MAX_CACHED_LIST_PAGE = 2;

	private final ProductRepository productRepository;
	private final ProductCacheRepository productCacheRepository;
	private final ProductCacheMetrics productCacheMetrics;
	private final ConcurrentHashMap<String, CompletableFuture<Page<ProductInfo>>> listLoadInFlight = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, CompletableFuture<ProductInfo>> detailLoadInFlight = new ConcurrentHashMap<>();

	public Page<ProductInfo> getProductList(final ProductData.GetProductList data) {
		if (!isListCacheable(data.pageable())) {
			return productRepository.getProductList(data.brandId(), data.pageable());
		}

		Optional<CacheablePage<ProductInfo>> cached = productCacheRepository.findProductList(data.brandId(), data.pageable());
		if (cached.isPresent()) {
			productCacheMetrics.recordCacheHit();
			if (log.isDebugEnabled()) {
				String brandKey = data.brandId() == null ? "all" : String.valueOf(data.brandId().getBrandId());
				log.debug("Product list cache hit: brandId={}, page={}, size={}",
					brandKey, data.pageable().getPageNumber(), data.pageable().getPageSize());
			}
			return cached.get().toPage(data.pageable());
		}

		productCacheMetrics.recordCacheMiss();
		if (log.isDebugEnabled()) {
			String brandKey = data.brandId() == null ? "all" : String.valueOf(data.brandId().getBrandId());
			log.debug("Product list cache miss: brandId={}, page={}, size={}",
				brandKey, data.pageable().getPageNumber(), data.pageable().getPageSize());
		}

		return executeSingleFlight(
			listLoadInFlight,
			buildListLoadKey(data.brandId(), data.pageable()),
			() -> loadProductListFromDatabaseAndCache(data)
		);
	}

	public ProductInfo getProductDetail(final ProductId productId) {
		Optional<ProductInfo> cached = productCacheRepository.findProductDetail(productId);
		if (cached.isPresent()) {
			productCacheMetrics.recordCacheHit();
			log.debug("Product detail cache hit: productId={}", productId.getProductId());
			return cached.get();
		}

		productCacheMetrics.recordCacheMiss();
		log.debug("Product detail cache miss: productId={}", productId.getProductId());
		return executeSingleFlight(
			detailLoadInFlight,
			buildDetailLoadKey(productId),
			() -> loadProductDetailFromDatabaseAndCache(productId)
		);
	}

	@Transactional(readOnly = true)
	public Map<ProductId, ProductInfo> getProductByIds(final List<ProductId> productIds) {
		if (productIds == null || productIds.isEmpty()) {
			return Map.of();
		}

		List<ProductId> uniqueProductIds = deduplicateProductIds(productIds);
		if (uniqueProductIds.isEmpty()) {
			return Map.of();
		}

		Map<ProductId, ProductInfo> productsById = new HashMap<>(
			productCacheRepository.findProductDetailsByIds(uniqueProductIds));
		int redisHitCount = productsById.size();

		List<ProductId> uncachedProductIds = uniqueProductIds.stream()
			.filter(productId -> !productsById.containsKey(productId))
			.toList();
		int cacheMissCount = uncachedProductIds.size();

		productCacheMetrics.recordBatchCacheHitCount(redisHitCount);
		productCacheMetrics.recordBatchCacheMissCount(cacheMissCount);

		if (!uncachedProductIds.isEmpty()) {
			loadProductsByIdsFromDatabaseAndCache(uncachedProductIds, productsById);
		}

		if (log.isDebugEnabled()) {
			log.debug("getProductByIds - requested: {}, unique: {}, redisHits: {}, cacheMisses: {}",
				productIds.size(), uniqueProductIds.size(), redisHitCount, cacheMissCount);
		}

		return orderProductsByRequest(uniqueProductIds, productsById);
	}

	private boolean isListCacheable(final Pageable pageable) {
		int pageNumber = pageable.getPageNumber();
		return pageNumber >= 0 && pageNumber <= MAX_CACHED_LIST_PAGE;
	}

	private Page<ProductInfo> loadProductListFromDatabaseAndCache(final ProductData.GetProductList data) {
		productCacheMetrics.recordDbLoad();
		Page<ProductInfo> dbResult = productRepository.getProductList(data.brandId(), data.pageable());
		productCacheRepository.saveProductList(data.brandId(), data.pageable(), CacheablePage.from(dbResult));
		return dbResult;
	}

	private ProductInfo loadProductDetailFromDatabaseAndCache(final ProductId productId) {
		productCacheMetrics.recordDbLoad();
		ProductInfo productInfo = productRepository.findById(productId)
			.orElseThrow(
				() -> new CoreException(ErrorType.NOT_FOUND, ErrorMessage.PRODUCT_NOT_FOUND.format(productId.getProductId())));
		productCacheRepository.saveProductDetail(productId, productInfo);
		return productInfo;
	}

	private void loadProductsByIdsFromDatabaseAndCache(
		final List<ProductId> uncachedProductIds,
		final Map<ProductId, ProductInfo> productsById
	) {
		productCacheMetrics.recordDbLoad();
		List<ProductInfo> productInfos = productRepository.findInfosByIds(uncachedProductIds);
		productCacheRepository.saveProductDetails(productInfos);
		for (ProductInfo productInfo : productInfos) {
			productsById.put(ProductId.of(productInfo.productId()), productInfo);
		}
	}

	private Map<ProductId, ProductInfo> orderProductsByRequest(
		final List<ProductId> requestedProductIds,
		final Map<ProductId, ProductInfo> productsById
	) {
		Map<ProductId, ProductInfo> orderedProducts = new LinkedHashMap<>();
		for (ProductId productId : requestedProductIds) {
			ProductInfo productInfo = productsById.get(productId);
			if (productInfo != null) {
				orderedProducts.put(productId, productInfo);
			}
		}
		return orderedProducts;
	}

	private List<ProductId> deduplicateProductIds(final List<ProductId> productIds) {
		Map<Long, ProductId> uniqueById = new LinkedHashMap<>();
		for (ProductId productId : productIds) {
			if (productId == null) {
				continue;
			}
			uniqueById.putIfAbsent(productId.getProductId(), productId);
		}
		return List.copyOf(uniqueById.values());
	}

	private String buildListLoadKey(final BrandId brandId, final Pageable pageable) {
		String brandKey = brandId == null ? "all" : String.valueOf(brandId.getBrandId());
		return brandKey + ":" + pageable.getPageNumber() + ":" + pageable.getPageSize() + ":" + pageable.getSort();
	}

	private String buildDetailLoadKey(final ProductId productId) {
		return String.valueOf(productId.getProductId());
	}

	private <T> T executeSingleFlight(
		final ConcurrentHashMap<String, CompletableFuture<T>> inFlight,
		final String key,
		final Supplier<T> loader
	) {
		CompletableFuture<T> newFuture = new CompletableFuture<>();
		CompletableFuture<T> existingFuture = inFlight.putIfAbsent(key, newFuture);

		if (existingFuture != null) {
			try {
				return existingFuture.join();
			} catch (CompletionException completionException) {
				throw toRuntimeException(completionException.getCause() == null
					? completionException
					: completionException.getCause());
			}
		}

		try {
			T loaded = loader.get();
			newFuture.complete(loaded);
			return loaded;
		} catch (Throwable throwable) {
			newFuture.completeExceptionally(throwable);
			throw toRuntimeException(throwable);
		} finally {
			inFlight.remove(key, newFuture);
		}
	}

	private RuntimeException toRuntimeException(final Throwable throwable) {
		if (throwable instanceof RuntimeException runtimeException) {
			return runtimeException;
		}
		return new IllegalStateException(throwable);
	}

}
