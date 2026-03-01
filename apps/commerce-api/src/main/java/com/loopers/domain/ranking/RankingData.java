package com.loopers.domain.ranking;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;

public record RankingData() {

	public record GetRanking(
		String userId,
		LocalDate date,
		String period,
		Pageable pageable
	) {
	}
}
