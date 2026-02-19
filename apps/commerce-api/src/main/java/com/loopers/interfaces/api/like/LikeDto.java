package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeResult;

import io.swagger.v3.oas.annotations.media.Schema;

public record LikeDto() {

	public record V1() {

		public record LikeResponse(
			@Schema(description = "회원 아이디", example = "loopers01")
			String userId,
			@Schema(description = "상품 ID", example = "100")
			Long productId
		) {
			public static LikeResponse from(final LikeResult likeResult) {
				return new LikeResponse(likeResult.userId(), likeResult.productId());
			}
		}

	}
}
