package com.loopers.application.ranking;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;

public record GetRankingQuery(
	String userId,
	LocalDate date,
	String period,
	Pageable pageable
) {
}
