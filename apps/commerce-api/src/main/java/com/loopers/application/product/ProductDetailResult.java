package com.loopers.application.product;

import com.loopers.domain.product.ProductInfo;

public record ProductDetailResult(
	Long productId,
	String productName,
	Long price,
	Long quantity,
	String brandName,
	Long likeCount,
	Long orderCount,
	Long viewCount,
	Long rank
) {
	public static ProductDetailResult from(final ProductInfo productInfo, final Long rank) {
		return new ProductDetailResult(
			productInfo.productId(),
			productInfo.productName(),
			productInfo.price(),
			productInfo.quantity(),
			productInfo.brandName(),
			productInfo.likeCount(),
			productInfo.orderCount(),
			productInfo.viewCount(),
			rank
		);
	}
}
