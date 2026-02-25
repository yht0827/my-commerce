package com.loopers.infrastructure.ranking;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.loopers.domain.ranking.RankingItem;
import com.loopers.domain.ranking.RankingPeriod;
import com.loopers.domain.ranking.RankingSnapshotRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RankingSnapshotRepositoryImpl implements RankingSnapshotRepository {

	private final RankingSnapshotJpaRepository rankingSnapshotJpaRepository;

	@Override
	public Page<RankingItem> fetchDaily(LocalDate date, Pageable pageable) {
		return rankingSnapshotJpaRepository.findByRankTypeAndRankDate(RankingPeriod.DAILY, date, pageable);
	}

	@Override
	public Page<RankingItem> fetchWeekly(LocalDate rankDate, Pageable pageable) {
		return rankingSnapshotJpaRepository.findByRankTypeAndRankDate(RankingPeriod.WEEKLY, rankDate, pageable);
	}

	@Override
	public Page<RankingItem> fetchMonthly(LocalDate rankDate, Pageable pageable) {
		return rankingSnapshotJpaRepository.findByRankTypeAndRankDate(RankingPeriod.MONTHLY, rankDate, pageable);
	}
}
