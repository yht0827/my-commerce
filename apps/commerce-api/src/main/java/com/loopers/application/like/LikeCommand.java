package com.loopers.application.like;

import com.loopers.domain.like.LikeData;

public record LikeCommand() {

	public record LikeProduct(String userId, Long productId) {
		public LikeData.LikeProduct toData() {
			return new LikeData.LikeProduct(userId, productId);
		}
	}

	public record UnlikeProduct(String userId, Long productId) {
		public LikeData.UnlikeProduct toData() {
			return new LikeData.UnlikeProduct(userId, productId);
		}
	}

}
