package com.loopers.domain.ranking;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RankingSnapshotRepository {

	Page<RankingItem> fetchDaily(LocalDate date, Pageable pageable);

	Page<RankingItem> fetchWeekly(LocalDate rankDate, Pageable pageable);

	Page<RankingItem> fetchMonthly(LocalDate rankDate, Pageable pageable);
}
