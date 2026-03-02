package com.loopers.interfaces.runner;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import com.loopers.domain.ranking.RankingItem;
import com.loopers.domain.ranking.RankingSnapshotRepository;
import com.loopers.support.ranking.RankingKeyManger;
import com.loopers.support.ranking.RankingType;

@DisplayName("RankingWarmupRunner 테스트")
class RankingWarmupRunnerTest {

	private RankingSnapshotRepository rankingSnapshotRepository;
	private RankingKeyManger rankingKeyManger;
	private StringRedisTemplate rankingStringRedisTemplate;
	private ZSetOperations<String, String> zSetOperations;

	private RankingWarmupRunner rankingWarmupRunner;

	@BeforeEach
	void setUp() {
		rankingSnapshotRepository = mock(RankingSnapshotRepository.class);
		rankingKeyManger = mock(RankingKeyManger.class);
		rankingStringRedisTemplate = mock(StringRedisTemplate.class);
		zSetOperations = mock(ZSetOperations.class);

		when(rankingStringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
		when(rankingKeyManger.getTTL(any())).thenReturn(172_800L);

		rankingWarmupRunner = new RankingWarmupRunner(
			rankingSnapshotRepository,
			rankingKeyManger,
			rankingStringRedisTemplate
		);
	}

	@Test
	@DisplayName("Redis 키가 이미 존재하면 일간 랭킹 적재를 건너뛴다")
	void warmupDaily_skipsWhenRedisKeyAlreadyPopulated() throws Exception {
		String key = "ranking:daily:20260303";
		when(rankingKeyManger.getDailyRankingKey(any())).thenReturn(key);
		when(zSetOperations.size(key)).thenReturn(10L);

		rankingWarmupRunner.run(null);

		verify(rankingSnapshotRepository, never()).fetchDaily(any(), any());
		verify(zSetOperations, never()).add(eq(key), anyString(), anyDouble());
	}

	@Test
	@DisplayName("Redis 키가 비어있고 DB 스냅샷이 있으면 일간 랭킹을 Redis에 적재한다")
	void warmupDaily_loadsFromSnapshotWhenRedisIsEmpty() throws Exception {
		String key = "ranking:daily:20260303";
		LocalDate today = LocalDate.now();

		when(rankingKeyManger.getDailyRankingKey(any())).thenReturn(key);
		when(rankingKeyManger.getWeeklyRankingKey()).thenReturn("ranking:weekly");
		when(rankingKeyManger.getMonthlyRankingKey()).thenReturn("ranking:monthly");
		when(zSetOperations.size(key)).thenReturn(0L);
		when(zSetOperations.size("ranking:weekly")).thenReturn(10L);
		when(zSetOperations.size("ranking:monthly")).thenReturn(10L);

		RankingItem item = RankingItem.create()
			.productId(1L).score(5.0).rank(1L).build();
		Page<RankingItem> snapshot = new PageImpl<>(List.of(item));
		when(rankingSnapshotRepository.fetchDaily(any(LocalDate.class), eq(Pageable.unpaged())))
			.thenReturn(snapshot);

		rankingWarmupRunner.run(null);

		verify(zSetOperations).add(key, "1", 5.0);
		verify(rankingStringRedisTemplate).expire(eq(key), any());
	}

	@Test
	@DisplayName("DB 스냅샷이 비어있으면 일간 랭킹 적재를 건너뛴다")
	void warmupDaily_skipsWhenSnapshotIsEmpty() throws Exception {
		String key = "ranking:daily:20260303";

		when(rankingKeyManger.getDailyRankingKey(any())).thenReturn(key);
		when(rankingKeyManger.getWeeklyRankingKey()).thenReturn("ranking:weekly");
		when(rankingKeyManger.getMonthlyRankingKey()).thenReturn("ranking:monthly");
		when(zSetOperations.size(key)).thenReturn(0L);
		when(zSetOperations.size("ranking:weekly")).thenReturn(10L);
		when(zSetOperations.size("ranking:monthly")).thenReturn(10L);
		when(rankingSnapshotRepository.fetchDaily(any(), any())).thenReturn(Page.empty());

		rankingWarmupRunner.run(null);

		verify(zSetOperations, never()).add(eq(key), anyString(), anyDouble());
	}

	@Test
	@DisplayName("Redis 장애가 발생해도 앱 기동을 차단하지 않는다")
	void warmupDaily_doesNotPropagateExceptionOnRedisFailure() throws Exception {
		String key = "ranking:daily:20260303";

		when(rankingKeyManger.getDailyRankingKey(any())).thenReturn(key);
		when(rankingKeyManger.getWeeklyRankingKey()).thenReturn("ranking:weekly");
		when(rankingKeyManger.getMonthlyRankingKey()).thenReturn("ranking:monthly");
		when(zSetOperations.size(key)).thenThrow(new RuntimeException("Redis 연결 실패"));
		when(zSetOperations.size("ranking:weekly")).thenReturn(10L);
		when(zSetOperations.size("ranking:monthly")).thenReturn(10L);

		// 예외가 전파되지 않아야 한다
		org.assertj.core.api.Assertions.assertThatCode(() -> rankingWarmupRunner.run(null))
			.doesNotThrowAnyException();
	}
}
