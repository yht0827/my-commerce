package com.loopers.domain.ranking;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RedisRankingRepository {

	Page<RankingItem> fetchDaily(LocalDate date, Pageable pageable);

	Page<RankingItem> fetchWeekly(Pageable pageable);

	Page<RankingItem> fetchMonthly(Pageable pageable);

	Long fetchDailyRank(Long productId, LocalDate date);
}
