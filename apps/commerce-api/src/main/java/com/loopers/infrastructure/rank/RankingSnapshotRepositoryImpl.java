package com.loopers.infrastructure.rank;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.loopers.domain.rank.RankingItem;
import com.loopers.domain.rank.RankingSnapshotRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RankingSnapshotRepositoryImpl implements RankingSnapshotRepository {

	private static final DateTimeFormatter WEEKLY_PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final DateTimeFormatter MONTHLY_PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

	private final RankingSnapshotJpaRepository rankingSnapshotJpaRepository;

	@Override
	public Page<RankingItem> fetchDaily(LocalDate date, Pageable pageable) {
		return rankingSnapshotJpaRepository.findDaily(date, pageable);
	}

	@Override
	public Page<RankingItem> fetchWeekly(LocalDate weekStart, Pageable pageable) {
		String periodKey = WEEKLY_PERIOD_FORMAT.format(weekStart);
		return rankingSnapshotJpaRepository.findWeekly(periodKey, pageable);
	}

	@Override
	public Page<RankingItem> fetchMonthly(LocalDate monthStart, Pageable pageable) {
		String periodKey = MONTHLY_PERIOD_FORMAT.format(monthStart);
		return rankingSnapshotJpaRepository.findMonthly(periodKey, pageable);
	}
}
