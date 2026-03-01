package com.loopers.domain.like;

public record LikeInfo(
	String userId,
	Long productId
) {

	public static LikeInfo from(final Like like) {
		return new LikeInfo(
			like.getUserId().getUserId(),
			like.getProductId().getProductId()
		);
	}

}
