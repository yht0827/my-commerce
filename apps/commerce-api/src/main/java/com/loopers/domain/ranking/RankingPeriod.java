package com.loopers.domain.ranking;

import java.util.Arrays;

public enum RankingPeriod {
	DAILY,
	WEEKLY,
	MONTHLY,
	;

	public static RankingPeriod from(final String period) {
		if (period == null || period.isBlank()) {
			return DAILY;
		}

		return Arrays.stream(values())
			.filter(value -> value.name().equalsIgnoreCase(period))
			.findFirst()
			.orElseThrow(IllegalArgumentException::new);
	}
}
