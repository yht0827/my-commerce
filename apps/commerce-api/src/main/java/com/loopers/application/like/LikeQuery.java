package com.loopers.application.like;

import com.loopers.domain.like.LikeData;

public record LikeQuery() {

	public record GetLikedProducts(String userId) {
		public LikeData.GetLikedProducts toData() {
			return new LikeData.GetLikedProducts(userId);
		}
	}

}
