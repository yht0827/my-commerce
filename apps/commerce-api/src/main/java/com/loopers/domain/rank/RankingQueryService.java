package com.loopers.domain.rank;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingQueryService {

	private final RankingValidator rankingValidator;
	private final RedisRankingRepository redisRankingRepository;
	private final RankingSnapshotRepository rankingSnapshotRepository;

	public RankingPageData getDaily(LocalDate date, Pageable pageable) {
		LocalDate validatedDate = rankingValidator.validateDate(date);

		Page<RankingItem> realtimePage = redisRankingRepository.fetchDaily(validatedDate, pageable);
		if (realtimePage != null && !realtimePage.isEmpty()) {
			return new RankingPageData(realtimePage, RankingSource.REALTIME);
		}

		log.debug("Daily ranking fallback to snapshot - date={}, page={}, size={}",
			validatedDate, pageable.getPageNumber(), pageable.getPageSize());
		Page<RankingItem> snapshotPage = rankingSnapshotRepository.fetchDaily(validatedDate, pageable);
		return new RankingPageData(snapshotPage, RankingSource.SNAPSHOT);
	}

	public RankingPageData getWeekly(LocalDate date, Pageable pageable) {
		LocalDate validatedDate = rankingValidator.validateDate(date);
		LocalDate weekStart = validatedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

		Page<RankingItem> page = rankingSnapshotRepository.fetchWeekly(weekStart, pageable);
		return new RankingPageData(page, RankingSource.SNAPSHOT);
	}

	public RankingPageData getMonthly(LocalDate date, Pageable pageable) {
		LocalDate validatedDate = rankingValidator.validateDate(date);
		LocalDate monthStart = validatedDate.withDayOfMonth(1);

		Page<RankingItem> page = rankingSnapshotRepository.fetchMonthly(monthStart, pageable);
		return new RankingPageData(page, RankingSource.SNAPSHOT);
	}

	public Long getProductRanking(Long productId) {
		if (productId == null) {
			return null;
		}
		LocalDate today = rankingValidator.validateDate(LocalDate.now());
		return redisRankingRepository.fetchRank(productId, today);
	}
}
