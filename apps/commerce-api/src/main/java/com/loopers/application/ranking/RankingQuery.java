package com.loopers.application.ranking;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;

import com.loopers.domain.ranking.RankingData;

public record RankingQuery() {

	public record GetRanking(
		String userId,
		LocalDate date,
		String period,
		Pageable pageable
	) {
		public RankingData.GetRanking toData() {
			return new RankingData.GetRanking(userId, date, period, pageable);
		}
	}
}
