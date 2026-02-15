package com.loopers.interfaces.api.rank;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loopers.application.ranking.GetRankingQuery;
import com.loopers.application.ranking.RankingPageResult;
import com.loopers.application.ranking.RankingUseCase;
import com.loopers.interfaces.api.common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rankings")
public class RankingV1Controller implements RankingV1ApiSpec {

	private final RankingUseCase rankingUseCase;

	@GetMapping
	@Override
	public ApiResponse<RankingDto.V1.RankingPageResponse> getRanking(@RequestHeader("X-USER-ID") final String userId,
		@Valid final RankingDto.V1.RankingRequest request) {

		GetRankingQuery query = request.from(userId);

		RankingPageResult result = rankingUseCase.getRanking(query);

		RankingDto.V1.RankingPageResponse response = RankingDto.V1.RankingPageResponse.from(result);
		return ApiResponse.success(response);
	}
}
