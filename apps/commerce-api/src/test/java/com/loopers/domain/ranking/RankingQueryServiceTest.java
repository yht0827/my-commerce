package com.loopers.domain.ranking;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DisplayName("RankingQueryService 테스트")
class RankingQueryServiceTest {

	private final RankingValidator rankingValidator = mock(RankingValidator.class);
	private final RedisRankingRepository redisRankingRepository = mock(RedisRankingRepository.class);
	private final RankingSnapshotRepository rankingSnapshotRepository = mock(RankingSnapshotRepository.class);

	private final RankingQueryService rankingQueryService = new RankingQueryService(
		rankingValidator,
		redisRankingRepository,
		rankingSnapshotRepository
	);

	@Test
	@DisplayName("DAILY 조회 시 Redis 데이터가 있으면 REALTIME 소스를 반환한다")
	void getRanking_daily_returnsRealtimeWhenRedisHasData() {
		LocalDate date = LocalDate.of(2026, 2, 20);
		Pageable pageable = PageRequest.of(0, 10);
		RankingData.GetRanking query = new RankingData.GetRanking("u1", date, "DAILY", pageable);

		when(rankingValidator.validateDate(date)).thenReturn(date);
		when(rankingValidator.validatePeriod("DAILY")).thenReturn(RankingPeriod.DAILY);

		Page<RankingItem> redisPage = new PageImpl<>(List.of(
			new RankingItem(1L, 10L, 12.5, null, null, null)
		), pageable, 1);
		when(redisRankingRepository.fetchDaily(date, pageable)).thenReturn(redisPage);

		RankingInfo.RankingPage result = rankingQueryService.getRanking(query);

		assertThat(result.source()).isEqualTo(RankingSource.REALTIME);
		assertThat(result.items()).hasSize(1);
		verify(rankingSnapshotRepository, never()).fetchDaily(any(), any());
	}

	@Test
	@DisplayName("WEEKLY 조회 시 Redis 장애가 발생하면 Snapshot으로 fallback 한다")
	void getRanking_weekly_fallbacksToSnapshotWhenRedisFails() {
		LocalDate date = LocalDate.of(2026, 2, 20);
		Pageable pageable = PageRequest.of(0, 10);
		RankingData.GetRanking query = new RankingData.GetRanking("u1", date, "WEEKLY", pageable);

		when(rankingValidator.validateDate(date)).thenReturn(date);
		when(rankingValidator.validatePeriod("WEEKLY")).thenReturn(RankingPeriod.WEEKLY);
		when(redisRankingRepository.fetchWeekly(pageable)).thenThrow(new RuntimeException("redis down"));

		Page<RankingItem> snapshotPage = new PageImpl<>(List.of(
			new RankingItem(1L, 99L, 7.7, null, null, null)
		), pageable, 1);
		when(rankingSnapshotRepository.fetchWeekly(date, pageable)).thenReturn(snapshotPage);

		RankingInfo.RankingPage result = rankingQueryService.getRanking(query);

		assertThat(result.source()).isEqualTo(RankingSource.SNAPSHOT);
		assertThat(result.items()).hasSize(1);
		verify(rankingSnapshotRepository).fetchWeekly(date, pageable);
	}

	@Test
	@DisplayName("상품 랭킹 조회는 당일 Redis daily rank를 사용한다")
	void getProductRanking_usesDailyRank() {
		LocalDate today = LocalDate.of(2026, 2, 20);
		when(rankingValidator.validateDate(any(LocalDate.class))).thenReturn(today);
		when(redisRankingRepository.fetchDailyRank(10L, today)).thenReturn(3L);

		Long result = rankingQueryService.getProductRanking(10L);

		assertThat(result).isEqualTo(3L);
		verify(redisRankingRepository).fetchDailyRank(10L, today);
	}
}
