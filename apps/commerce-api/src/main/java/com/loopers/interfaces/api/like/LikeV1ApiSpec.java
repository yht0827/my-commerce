package com.loopers.interfaces.api.like;

import java.util.List;

import com.loopers.interfaces.api.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Likes V1 API", description = "Likes API 입니다.")
public interface LikeV1ApiSpec {

	@Operation(
		summary = "좋아요 등록",
		description = "상품에 좋아요를 등록합니다."
	)
	ApiResponse<LikeDto.V1.LikeResponse> likeProduct(final String userId, final Long productId);

	@Operation(
		summary = "좋아요 취소",
		description = "상품의 좋아요를 취소합니다."
	)
	ApiResponse<Void> unlikeProduct(final Long productId, final String userId);

	@Operation(
		summary = "좋아요 목록 조회",
		description = "사용자가 좋아요한 상품 목록을 조회합니다."
	)
	ApiResponse<List<LikeDto.V1.LikeResponse>> getLikedProductList(final String userId);
}
