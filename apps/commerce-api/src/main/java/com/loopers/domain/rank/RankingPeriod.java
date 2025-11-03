package com.loopers.domain.rank;

public enum RankingPeriod {
	DAILY,
	WEEKLY,
	MONTHLY,
	;

	public static RankingPeriod from(String period) {
		return RankingPeriod.valueOf(period.toUpperCase());
	}
}
