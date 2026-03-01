package com.loopers.domain.product;

import org.springframework.data.domain.Pageable;

import com.loopers.domain.brand.BrandId;

public record ProductData() {

	public record GetProductList(BrandId brandId, Pageable pageable) {
	}

	public record StockQuantityChanged(Long productId, Long previousQuantity, Long currentQuantity) {
	}
}
