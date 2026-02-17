package com.loopers.domain.product;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.order.OrderItem;
import com.loopers.domain.product.event.ProductOutOfStockEvent;
import com.loopers.support.error.ErrorMessage;
import com.loopers.support.cache.CacheablePage;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.support.event.EventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

	private static final int MAX_CACHED_LIST_PAGE = 2;

	private final ProductRepository productRepository;
	private final ProductCacheRepository productCacheRepository;
	private final EventPublisher eventPublisher;

	public Page<ProductInfo> getProductList(final ProductCommand.GetProductList command) {
		int pageNumber = command.pageable().getPageNumber();

		if (isListCacheable(pageNumber)) {
			return productCacheRepository.findProductList(command.brandId(), command.pageable())
				.map(cachedPage -> cachedPage.toPage(command.pageable()))
				.orElseGet(() -> loadProductListFromDatabaseAndCache(command));
		}

		return productRepository.getProductList(command.brandId(), command.pageable());
	}

	public ProductInfo getProductDetail(final ProductId productId) {
		return productCacheRepository.findProductDetail(productId)
			.orElseGet(() -> loadProductDetailFromDatabaseAndCache(productId));
	}

	@Transactional(readOnly = true)
	public Map<ProductId, ProductInfo> getProductByIds(final List<ProductId> productIds) {
		if (productIds == null || productIds.isEmpty()) {
			return new HashMap<>();
		}

		List<ProductId> uniqueProductIds = deduplicateProductIds(productIds);
		if (uniqueProductIds.isEmpty()) {
			return new HashMap<>();
		}

		Map<ProductId, ProductInfo> result = new HashMap<>();
		Map<ProductId, ProductInfo> cachedProducts = productCacheRepository.findProductDetailsByIds(uniqueProductIds);
		result.putAll(cachedProducts);

		List<ProductId> uncachedProductIds = uniqueProductIds.stream()
			.filter(productId -> !cachedProducts.containsKey(productId))
			.toList();

		if (!uncachedProductIds.isEmpty()) {
			loadProductsByIdsFromDatabaseAndCache(uncachedProductIds, result);
		}

		if (log.isDebugEnabled()) {
			log.debug("getProductByIds - requested: {}, unique: {}, redisHits: {}, cacheMisses: {}",
				productIds.size(), uniqueProductIds.size(), cachedProducts.size(), uncachedProductIds.size());
		}

		return result;
	}

	public void deductStock(final List<OrderItem> orderItems) {
		for (OrderItem item : orderItems) {
			deductStockItem(item);
		}
	}

	public void evictProductCache(final ProductId productId) {
		executeCacheEviction(
			() -> productCacheRepository.evictProductDetail(productId),
			() -> log.debug("상품 캐시 제거 완료 - productId: {}", productId.getProductId()),
			e -> log.warn("상품 캐시 제거 실패 - productId: {}", productId.getProductId(), e)
		);
	}

	public void evictProductListCache() {
		executeCacheEviction(
			productCacheRepository::evictProductList,
			() -> log.debug("상품 리스트 캐시 제거 완료"),
			e -> log.warn("상품 리스트 캐시 제거 실패", e)
		);
	}

	public void evictProductRelatedCaches(final ProductId productId) {
		evictProductCache(productId);
		evictProductListCache();
	}

	private boolean isListCacheable(final int pageNumber) {
		return pageNumber >= 0 && pageNumber <= MAX_CACHED_LIST_PAGE;
	}

	private Page<ProductInfo> loadProductListFromDatabaseAndCache(final ProductCommand.GetProductList command) {
		Page<ProductInfo> dbResult = productRepository.getProductList(command.brandId(), command.pageable());
		productCacheRepository.saveProductList(command.brandId(), command.pageable(), CacheablePage.from(dbResult));
		return dbResult;
	}

	private ProductInfo loadProductDetailFromDatabaseAndCache(final ProductId productId) {
		log.debug("Product detail cache miss: productId={}", productId.getProductId());
		ProductInfo productInfo = productRepository.findById(productId)
			.orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, ErrorMessage.PRODUCT_NOT_FOUND.format(productId.getProductId())));
		productCacheRepository.saveProductDetail(productId, productInfo);
		return productInfo;
	}

	private void loadProductsByIdsFromDatabaseAndCache(
		final List<ProductId> uncachedProductIds,
		final Map<ProductId, ProductInfo> result
	) {
		List<ProductInfo> productInfos = productRepository.findInfosByIds(uncachedProductIds);
		productCacheRepository.saveProductDetails(productInfos);
		for (ProductInfo productInfo : productInfos) {
			result.put(ProductId.of(productInfo.productId()), productInfo);
		}
	}

	private void deductStockItem(final OrderItem item) {
		ProductId productId = item.getProductId();
		Product product = findProductWithPessimisticLock(productId);
		long oldQuantity = product.getQuantity().getQuantity();

		product.deduct(item.getQuantity());
		publishOutOfStockEventIfNeeded(productId, oldQuantity, product.getQuantity().isOutOfStock());
	}

	private Product findProductWithPessimisticLock(final ProductId productId) {
		return productRepository.findByIdWithPessimisticLock(productId)
			.orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, ErrorMessage.PRODUCT_NOT_FOUND.format(productId.getProductId())));
	}

	private void publishOutOfStockEventIfNeeded(
		final ProductId productId,
		final long oldQuantity,
		final boolean outOfStock
	) {
		if (oldQuantity <= 0 || !outOfStock) {
			return;
		}

		ProductOutOfStockEvent event = ProductOutOfStockEvent.create(productId.getProductId());
		eventPublisher.publish(event);
		log.info("품절 이벤트 발행: productId={}", productId.getProductId());
	}

	private void executeCacheEviction(
		final Runnable evictionAction,
		final Runnable onSuccess,
		final Consumer<Exception> onFailure
	) {
		try {
			evictionAction.run();
			onSuccess.run();
		} catch (Exception e) {
			onFailure.accept(e);
		}
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
}
