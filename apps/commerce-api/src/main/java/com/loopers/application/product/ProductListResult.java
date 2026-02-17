package com.loopers.application.product;

import java.util.List;

import org.springframework.data.domain.Page;

import com.loopers.domain.product.ProductInfo;

public record ProductListResult(List<ProductSummaryResult> products, Integer totalPages, Long totalElements) {
	public static ProductListResult from(Page<ProductInfo> productInfo) {
		return new ProductListResult(
			productInfo.getContent().stream().map(ProductSummaryResult::from).toList(),
			productInfo.getTotalPages(),
			productInfo.getTotalElements()
		);
	}
}
