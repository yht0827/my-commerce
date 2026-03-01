package com.loopers.application.like;

import com.loopers.domain.like.LikeInfo;

public record LikeResult(String userId, Long productId) {

	public static LikeResult from(final LikeInfo likeInfo) {

		return new LikeResult(likeInfo.userId(), likeInfo.productId());
	}

}
