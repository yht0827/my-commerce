package com.loopers.infrastructure.product;

import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.loopers.domain.brand.BrandId;
import com.loopers.domain.product.ProductId;
import com.loopers.support.config.CacheVersionProperties;

@Component
public class ProductCacheKeys {

	private static final String PRODUCT_LIST_PREFIX = "productList";
	private static final String PRODUCT_DETAIL_PREFIX = "productDetail";
	private static final String ALL_BRANDS = "all";
	private static final String SEPARATOR = ":";
	private static final String SORT_SEPARATOR = ",";
	private static final String UNSORTED = "unsorted";

	private final String productVersion;

	public ProductCacheKeys(final CacheVersionProperties cacheVersionProperties) {
		this.productVersion = cacheVersionProperties.product();
	}

	public String buildProductListKey(final BrandId brandId, final Pageable pageable) {
		String cacheBrandId = brandId == null ? ALL_BRANDS : String.valueOf(brandId.getBrandId());
		return String.join(
			SEPARATOR,
			productVersion,
			PRODUCT_LIST_PREFIX,
			cacheBrandId,
			String.valueOf(pageable.getPageNumber()),
			String.valueOf(pageable.getPageSize()),
			buildSortKey(pageable)
		);
	}

	public String buildProductListPattern() {
		return productVersion + SEPARATOR + PRODUCT_LIST_PREFIX + SEPARATOR + "*";
	}

	public String buildProductDetailKey(final ProductId productId) {
		return productVersion + SEPARATOR + PRODUCT_DETAIL_PREFIX + SEPARATOR + productId.getProductId();
	}

	private String buildSortKey(final Pageable pageable) {
		if (pageable.getSort().isUnsorted()) {
			return UNSORTED;
		}

		return pageable.getSort().stream()
			.map(this::buildSortOrderKey)
			.collect(Collectors.joining(SORT_SEPARATOR));
	}

	private String buildSortOrderKey(final Sort.Order order) {
		return order.getProperty() + SEPARATOR + order.getDirection().name() + SEPARATOR
			+ order.getNullHandling().name() + SEPARATOR + order.isIgnoreCase();
	}
}
