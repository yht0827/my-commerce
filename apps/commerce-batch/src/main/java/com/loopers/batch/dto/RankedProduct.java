package com.loopers.batch.dto;

public record RankedProduct(
	Long productId,
	Double score,
	Long likeCount,
	Long viewCount,
	Long orderCount
) {
}
