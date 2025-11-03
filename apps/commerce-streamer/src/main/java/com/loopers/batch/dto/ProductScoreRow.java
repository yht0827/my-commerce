package com.loopers.batch.dto;

public record ProductScoreRow(
	Long productId,
	Long likeCount,
	Long viewCount,
	Long orderCount,
	Double weightedScore
) {
}
