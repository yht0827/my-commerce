package com.loopers.domain.like;

public record LikeData() {

	public record LikeProduct(String userId, Long productId) {
	}

	public record UnlikeProduct(String userId, Long productId) {
	}

	public record GetLikedProducts(String userId) {
	}

}
