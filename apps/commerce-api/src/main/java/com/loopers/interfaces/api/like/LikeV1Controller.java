package com.loopers.interfaces.api.like;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loopers.application.like.LikeCommand;
import com.loopers.application.like.LikeFacade;
import com.loopers.application.like.LikeQuery;
import com.loopers.application.like.LikeResult;
import com.loopers.interfaces.api.common.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/like")
@RequiredArgsConstructor
public class LikeV1Controller {
	private final LikeFacade likeFacade;

	@PostMapping("/products/{productId}")
	public ApiResponse<LikeResponse> likeProduct(@RequestHeader final String userId, @PathVariable final Long productId) {
		LikeCommand.LikeProduct command = new LikeCommand.LikeProduct(userId, productId);
		LikeResult likeResult = likeFacade.likeProduct(command);
		LikeResponse response = LikeResponse.from(likeResult);
		return ApiResponse.success(response);
	}

	@DeleteMapping("/products/{productId}")
	public ApiResponse<Void> unlikeProduct(@PathVariable Long productId, @RequestHeader final String userId) {
		LikeCommand.UnlikeProduct command = new LikeCommand.UnlikeProduct(userId, productId);
		likeFacade.unlikeProduct(command);
		return ApiResponse.success(null);
	}

	@GetMapping("/products")
	public ApiResponse<List<LikeResponse>> getLikedProductList(@RequestHeader final String userId) {
		LikeQuery.GetLikedProducts query = new LikeQuery.GetLikedProducts(userId);
		List<LikeResult> likedProductList = likeFacade.getLikedProductList(query);

		List<LikeResponse> response = likedProductList.stream().map(LikeResponse::from).toList();

		return ApiResponse.success(response);
	}

}
