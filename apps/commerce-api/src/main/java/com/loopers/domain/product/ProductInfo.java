package com.loopers.domain.product;

public record ProductInfo(
	Long productId,
	String productName,
	Long price,
	Long quantity,
	String brandName,
	Long likeCount,
	Long orderCount,
	Long viewCount
) {
}
