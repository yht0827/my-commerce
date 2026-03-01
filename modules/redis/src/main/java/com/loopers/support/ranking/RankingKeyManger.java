package com.loopers.support.ranking;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

@Component
public class RankingKeyManger {

	private static final String KEY_PREFIX = "ranking";
	private static final String DATE_FORMAT = "yyyyMMdd";
	private static final long DAILY_TTL = 3_024_000L;
	private static final long ROLLING_TTL = 172_800L;

	private final DateTimeFormatter dailyFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);

	public String getDailyRankingKey(final LocalDate date) {
		return String.format("%s:daily:%s", KEY_PREFIX, date.format(dailyFormatter));
	}

	public String getWeeklyRankingKey() {
		return KEY_PREFIX + ":weekly";
	}

	public String getMonthlyRankingKey() {
		return KEY_PREFIX + ":monthly";
	}

	public long getTTL(final RankingType type) {
		return switch (type) {
			case DAILY -> DAILY_TTL;
			case WEEKLY, MONTHLY -> ROLLING_TTL;
		};
	}

}
