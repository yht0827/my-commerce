package com.loopers.application.ranking;

import java.util.List;

import com.loopers.domain.ranking.RankingInfo;

public record RankingPageResult(
	int page, int size, long totalElements, int totalPages,
	List<RankingProductResult> items
) {
	public static RankingPageResult from(
		final RankingInfo.RankingPage pageData,
		final List<RankingProductResult> items
	) {

		return new RankingPageResult(
			pageData.page(), pageData.size(),
			pageData.totalElements(), pageData.totalPages(),
			items
		);

	}
}
