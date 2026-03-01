package com.loopers.interfaces.api.ranking;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loopers.application.ranking.RankingApplicationService;
import com.loopers.application.ranking.RankingPageResult;
import com.loopers.application.ranking.RankingQuery;
import com.loopers.interfaces.api.common.ApiResponse;
import com.loopers.interfaces.api.common.CurrentUserId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rankings")
public class RankingV1Controller implements RankingV1ApiSpec {

	private final RankingApplicationService rankingApplicationService;

	@GetMapping
	@Override
	public ApiResponse<RankingDto.V1.RankingPageResponse> getRanking(@CurrentUserId final String userId,
		@Valid final RankingDto.V1.RankingRequest request) {

		RankingQuery.GetRanking query = request.toQuery(userId);

		RankingPageResult result = rankingApplicationService.getRanking(query);

		RankingDto.V1.RankingPageResponse response = RankingDto.V1.RankingPageResponse.from(result);
		return ApiResponse.success(response);
	}
}
