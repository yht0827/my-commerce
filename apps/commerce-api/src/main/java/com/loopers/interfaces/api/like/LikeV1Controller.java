package com.loopers.interfaces.api.like;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loopers.application.like.LikeApplicationService;
import com.loopers.application.like.LikeCommand;
import com.loopers.application.like.LikeQuery;
import com.loopers.application.like.LikeResult;
import com.loopers.interfaces.api.common.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/like")
@RequiredArgsConstructor
public class LikeV1Controller implements LikeV1ApiSpec {
	private final LikeApplicationService likeApplicationService;

	@PostMapping("/products/{productId}")
    @Override
	public ApiResponse<LikeDto.V1.LikeResponse> likeProduct(
		@RequestHeader final String userId,
		@PathVariable final Long productId
	) {
		LikeCommand.LikeProduct command = new LikeCommand.LikeProduct(userId, productId);
		LikeResult likeResult = likeApplicationService.likeProduct(command);
		LikeDto.V1.LikeResponse response = LikeDto.V1.LikeResponse.from(likeResult);
		return ApiResponse.success(response);
	}

	@DeleteMapping("/products/{productId}")
    @Override
	public ApiResponse<Void> unlikeProduct(@PathVariable final Long productId, @RequestHeader final String userId) {
		LikeCommand.UnlikeProduct command = new LikeCommand.UnlikeProduct(userId, productId);
		likeApplicationService.unlikeProduct(command);
		return ApiResponse.success(null);
	}

	@GetMapping("/products")
    @Override
	public ApiResponse<List<LikeDto.V1.LikeResponse>> getLikedProductList(@RequestHeader final String userId) {
		LikeQuery.GetLikedProducts query = new LikeQuery.GetLikedProducts(userId);
		List<LikeResult> likedProductList = likeApplicationService.getLikedProductList(query);

		List<LikeDto.V1.LikeResponse> response = likedProductList.stream()
			.map(LikeDto.V1.LikeResponse::from)
			.toList();

		return ApiResponse.success(response);
	}

}
