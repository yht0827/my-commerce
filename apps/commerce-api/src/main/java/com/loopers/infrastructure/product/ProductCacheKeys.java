package com.loopers.infrastructure.product;

import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.loopers.domain.brand.BrandId;
import com.loopers.domain.product.ProductId;

final class ProductCacheKeys {

	private static final String PRODUCT_LIST_PREFIX = "productList";
	private static final String PRODUCT_DETAIL_PREFIX = "productDetail";
	private static final String ALL_BRANDS = "all";
	private static final String SEPARATOR = ":";
	private static final String SORT_SEPARATOR = ",";
	private static final String UNSORTED = "unsorted";

	private ProductCacheKeys() {
	}

	static String buildProductListKey(final BrandId brandId, final Pageable pageable) {
		String cacheBrandId = brandId == null ? ALL_BRANDS : String.valueOf(brandId.getBrandId());
		return String.join(
			SEPARATOR,
			PRODUCT_LIST_PREFIX,
			cacheBrandId,
			String.valueOf(pageable.getPageNumber()),
			String.valueOf(pageable.getPageSize()),
			buildSortKey(pageable)
		);
	}

	static String buildProductListPattern() {
		return PRODUCT_LIST_PREFIX + SEPARATOR + "*";
	}

	static String buildProductDetailKey(final ProductId productId) {
		return PRODUCT_DETAIL_PREFIX + SEPARATOR + productId.getProductId();
	}

	private static String buildSortKey(final Pageable pageable) {
		if (pageable.getSort().isUnsorted()) {
			return UNSORTED;
		}

		return pageable.getSort().stream()
			.map(ProductCacheKeys::buildSortOrderKey)
			.collect(Collectors.joining(SORT_SEPARATOR));
	}

	private static String buildSortOrderKey(final Sort.Order order) {
		return order.getProperty() + SEPARATOR + order.getDirection().name() + SEPARATOR
			+ order.getNullHandling().name() + SEPARATOR + order.isIgnoreCase();
	}
}
