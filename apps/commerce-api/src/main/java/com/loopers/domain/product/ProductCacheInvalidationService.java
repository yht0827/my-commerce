package com.loopers.domain.product;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCacheInvalidationService {

	private final ProductCacheRepository productCacheRepository;

	public void evictProductCache(final ProductId productId) {
		try {
			productCacheRepository.evictProductDetail(productId);
			log.debug("상품 캐시 제거 완료 - productId: {}", productId.getProductId());
		} catch (Exception e) {
			log.warn("상품 캐시 제거 실패 - productId: {}", productId.getProductId(), e);
		}
	}

	public void evictProductListCache() {
		try {
			productCacheRepository.evictProductList();
			log.debug("상품 리스트 캐시 제거 완료");
		} catch (Exception e) {
			log.warn("상품 리스트 캐시 제거 실패", e);
		}
	}

	public void evictProductRelatedCaches(final ProductId productId) {
		evictProductCache(productId);
		evictProductListCache();
	}

}
