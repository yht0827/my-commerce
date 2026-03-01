package com.loopers.interfaces.api.ranking;

import com.loopers.interfaces.api.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Rankings V1 API", description = "Rankings API 입니다.")
public interface RankingV1ApiSpec {

	@Operation(
		summary = "랭킹 Page 조회",
		description = "랭킹 Page를 조회 합니다."
	)
	ApiResponse<RankingDto.V1.RankingPageResponse> getRanking(final String userId,
		final RankingDto.V1.RankingRequest request);
}
