package com.loopers.domain.rank;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RedisRankingRepository {

	Page<RankingItem> fetchDaily(LocalDate date, Pageable pageable);

	Long fetchRank(Long productId, LocalDate date);
}
