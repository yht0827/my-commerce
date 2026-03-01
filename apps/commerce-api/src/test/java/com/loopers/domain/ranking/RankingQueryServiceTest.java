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
	@DisplayName("WEEKLY 조회 시 Redis 데이터가 있으면 REALTIME 소스를 반환한다")
	void getRanking_weekly_returnsRealtimeWhenRedisHasData() {
		LocalDate date = LocalDate.of(2026, 2, 20);
		Pageable pageable = PageRequest.of(0, 10);
		RankingData.GetRanking query = new RankingData.GetRanking("u1", date, "WEEKLY", pageable);

		when(rankingValidator.validateDate(date)).thenReturn(date);
		when(rankingValidator.validatePeriod("WEEKLY")).thenReturn(RankingPeriod.WEEKLY);
		Page<RankingItem> redisPage = new PageImpl<>(List.of(new RankingItem(1L, 71L, 8.8, null, null, null)), pageable, 1);
		when(redisRankingRepository.fetchWeekly(pageable)).thenReturn(redisPage);

		RankingInfo.RankingPage result = rankingQueryService.getRanking(query);

		assertThat(result.source()).isEqualTo(RankingSource.REALTIME);
		assertThat(result.items()).hasSize(1);
		verify(rankingSnapshotRepository, never()).fetchWeekly(any(), any());
	}

	@Test
	@DisplayName("DAILY 조회 시 Redis가 비어있으면 Snapshot으로 fallback 한다")
	void getRanking_daily_fallbacksToSnapshotWhenRedisEmpty() {
		LocalDate date = LocalDate.of(2026, 2, 20);
		Pageable pageable = PageRequest.of(0, 10);
		RankingData.GetRanking query = new RankingData.GetRanking("u1", date, "DAILY", pageable);

		when(rankingValidator.validateDate(date)).thenReturn(date);
		when(rankingValidator.validatePeriod("DAILY")).thenReturn(RankingPeriod.DAILY);
		when(redisRankingRepository.fetchDaily(date, pageable)).thenReturn(Page.empty(pageable));

		Page<RankingItem> snapshotPage = new PageImpl<>(List.of(
			new RankingItem(1L, 77L, 5.5, null, null, null)
		), pageable, 1);
		when(rankingSnapshotRepository.fetchDaily(date, pageable)).thenReturn(snapshotPage);

		RankingInfo.RankingPage result = rankingQueryService.getRanking(query);

		assertThat(result.source()).isEqualTo(RankingSource.SNAPSHOT);
		assertThat(result.items()).hasSize(1);
		verify(rankingSnapshotRepository).fetchDaily(date, pageable);
	}

	@Test
	@DisplayName("MONTHLY 조회 시 Redis가 비어있으면 Snapshot으로 fallback 한다")
	void getRanking_monthly_fallbacksToSnapshotWhenRedisEmpty() {
		LocalDate date = LocalDate.of(2026, 2, 20);
		Pageable pageable = PageRequest.of(0, 10);
		RankingData.GetRanking query = new RankingData.GetRanking("u1", date, "MONTHLY", pageable);

		when(rankingValidator.validateDate(date)).thenReturn(date);
		when(rankingValidator.validatePeriod("MONTHLY")).thenReturn(RankingPeriod.MONTHLY);
		when(redisRankingRepository.fetchMonthly(pageable)).thenReturn(Page.empty(pageable));

		Page<RankingItem> snapshotPage = new PageImpl<>(List.of(
			new RankingItem(1L, 88L, 6.6, null, null, null)
		), pageable, 1);
		when(rankingSnapshotRepository.fetchMonthly(date, pageable)).thenReturn(snapshotPage);

		RankingInfo.RankingPage result = rankingQueryService.getRanking(query);

		assertThat(result.source()).isEqualTo(RankingSource.SNAPSHOT);
		assertThat(result.items()).hasSize(1);
		verify(rankingSnapshotRepository).fetchMonthly(date, pageable);
	}

	@Test
	@DisplayName("MONTHLY 조회 시 Redis 데이터가 있으면 REALTIME 소스를 반환한다")
	void getRanking_monthly_returnsRealtimeWhenRedisHasData() {
		LocalDate date = LocalDate.of(2026, 2, 20);
		Pageable pageable = PageRequest.of(0, 10);
		RankingData.GetRanking query = new RankingData.GetRanking("u1", date, "MONTHLY", pageable);

		when(rankingValidator.validateDate(date)).thenReturn(date);
		when(rankingValidator.validatePeriod("MONTHLY")).thenReturn(RankingPeriod.MONTHLY);
		Page<RankingItem> redisPage = new PageImpl<>(List.of(new RankingItem(1L, 55L, 7.7, null, null, null)), pageable, 1);
		when(redisRankingRepository.fetchMonthly(pageable)).thenReturn(redisPage);

		RankingInfo.RankingPage result = rankingQueryService.getRanking(query);

		assertThat(result.source()).isEqualTo(RankingSource.REALTIME);
		assertThat(result.items()).hasSize(1);
		verify(rankingSnapshotRepository, never()).fetchMonthly(any(), any());
	}

	@Test
	@DisplayName("MONTHLY 조회 시 Redis 장애가 발생하면 Snapshot으로 fallback 한다")
	void getRanking_monthly_fallbacksToSnapshotWhenRedisFails() {
		LocalDate date = LocalDate.of(2026, 2, 20);
		Pageable pageable = PageRequest.of(0, 10);
		RankingData.GetRanking query = new RankingData.GetRanking("u1", date, "MONTHLY", pageable);

		when(rankingValidator.validateDate(date)).thenReturn(date);
		when(rankingValidator.validatePeriod("MONTHLY")).thenReturn(RankingPeriod.MONTHLY);
		when(redisRankingRepository.fetchMonthly(pageable)).thenThrow(new RuntimeException("redis down"));
		Page<RankingItem> snapshotPage = new PageImpl<>(List.of(new RankingItem(1L, 66L, 6.6, null, null, null)), pageable, 1);
		when(rankingSnapshotRepository.fetchMonthly(date, pageable)).thenReturn(snapshotPage);

		RankingInfo.RankingPage result = rankingQueryService.getRanking(query);

		assertThat(result.source()).isEqualTo(RankingSource.SNAPSHOT);
		assertThat(result.items()).hasSize(1);
		verify(rankingSnapshotRepository).fetchMonthly(date, pageable);
	}

	@Test
	@DisplayName("Snapshot도 null이면 빈 페이지를 반환한다")
	void getRanking_returnsEmptyPageWhenSnapshotIsNull() {
		LocalDate date = LocalDate.of(2026, 2, 20);
		Pageable pageable = PageRequest.of(0, 10);
		RankingData.GetRanking query = new RankingData.GetRanking("u1", date, "DAILY", pageable);

		when(rankingValidator.validateDate(date)).thenReturn(date);
		when(rankingValidator.validatePeriod("DAILY")).thenReturn(RankingPeriod.DAILY);
		when(redisRankingRepository.fetchDaily(date, pageable)).thenReturn(null);
		when(rankingSnapshotRepository.fetchDaily(date, pageable)).thenReturn(null);

		RankingInfo.RankingPage result = rankingQueryService.getRanking(query);

		assertThat(result.source()).isEqualTo(RankingSource.SNAPSHOT);
		assertThat(result.items()).isEmpty();
		assertThat(result.totalElements()).isZero();
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

	@Test
	@DisplayName("상품 ID가 null이면 null을 반환하고 Redis를 조회하지 않는다")
	void getProductRanking_returnsNullWhenProductIdIsNull() {
		Long result = rankingQueryService.getProductRanking(null);

		assertThat(result).isNull();
		verifyNoInteractions(redisRankingRepository);
	}
}
