package com.loopers.application.product;

import com.loopers.domain.product.ProductInfo;

public record ProductSummaryResult(
	Long productId,
	String productName,
	Long price,
	Long quantity,
	String brandName,
	Long likeCount,
	Long orderCount,
	Long viewCount
) {
	public static ProductSummaryResult from(final ProductInfo productInfo) {
		return new ProductSummaryResult(
			productInfo.productId(),
			productInfo.productName(),
			productInfo.price(),
			productInfo.quantity(),
			productInfo.brandName(),
			productInfo.likeCount(),
			productInfo.orderCount(),
			productInfo.viewCount()
		);
	}
}
