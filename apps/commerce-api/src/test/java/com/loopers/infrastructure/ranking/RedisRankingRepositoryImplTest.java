package com.loopers.infrastructure.ranking;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.loopers.domain.ranking.RankingItem;
import com.loopers.support.ranking.RankingKeyManger;

@DisplayName("RedisRankingRepositoryImpl 테스트")
@ExtendWith(MockitoExtension.class)
class RedisRankingRepositoryImplTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	@SuppressWarnings("unchecked")
    private ZSetOperations<String, String> zSetOperations;

	@Mock
	private RankingKeyManger rankingKeyManger;

	@InjectMocks
	private RedisRankingRepositoryImpl repository;

	@Test
	@DisplayName("zCard가 null이면 빈 페이지를 반환한다")
	void fetchDaily_returnsEmptyPageWhenTotalIsNull() {
		LocalDate date = LocalDate.of(2026, 3, 1);
		Pageable pageable = PageRequest.of(0, 10);
		String key = "ranking:daily:20260301";

		when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
		when(rankingKeyManger.getDailyRankingKey(date)).thenReturn(key);
		when(zSetOperations.zCard(key)).thenReturn(null);

		Page<RankingItem> result = repository.fetchDaily(date, pageable);

		assertThat(result).isEmpty();
		verify(zSetOperations, never()).reverseRangeWithScores(anyString(), anyLong(), anyLong());
	}

	@Test
	@DisplayName("요청 offset이 총 건수보다 크면 빈 페이지를 반환한다")
	void fetchDaily_returnsEmptyPageWhenOffsetOutOfRange() {
		LocalDate date = LocalDate.of(2026, 3, 1);
		Pageable pageable = PageRequest.of(1, 10);
		String key = "ranking:daily:20260301";

		when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
		when(rankingKeyManger.getDailyRankingKey(date)).thenReturn(key);
		when(zSetOperations.zCard(key)).thenReturn(1L);
		when(zSetOperations.reverseRangeWithScores(key, 10, 19)).thenReturn(Set.of());

		Page<RankingItem> result = repository.fetchDaily(date, pageable);

		assertThat(result.getContent()).isEmpty();
		assertThat(result.getTotalElements()).isEqualTo(1L);
	}

	@Test
	@DisplayName("정상 튜플만 랭킹 아이템으로 변환하고 잘못된 튜플은 건너뛴다")
	void fetchDaily_convertsValidTuplesAndSkipsInvalidTuples() {
		LocalDate date = LocalDate.of(2026, 3, 1);
		Pageable pageable = PageRequest.of(0, 10);
		String key = "ranking:daily:20260301";

		Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
		tuples.add(new DefaultTypedTuple<>("101", 9.8));
		tuples.add(new DefaultTypedTuple<>("not-a-number", 8.4));
		tuples.add(new DefaultTypedTuple<>("202", null));
		tuples.add(null);

		when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
		when(rankingKeyManger.getDailyRankingKey(date)).thenReturn(key);
		when(zSetOperations.zCard(key)).thenReturn(4L);
		when(zSetOperations.reverseRangeWithScores(key, 0, 9)).thenReturn(tuples);

		Page<RankingItem> result = repository.fetchDaily(date, pageable);

		assertThat(result.getContent()).hasSize(1);
		RankingItem first = result.getContent().getFirst();
		assertThat(ReflectionTestUtils.getField(first, "rank")).isEqualTo(1L);
		assertThat(ReflectionTestUtils.getField(first, "productId")).isEqualTo(101L);
		assertThat(ReflectionTestUtils.getField(first, "score")).isEqualTo(9.8);
	}

	@Test
	@DisplayName("weekly/monthly 조회는 각각의 키를 사용한다")
	void fetchWeeklyAndMonthly_useExpectedKeys() {
		Pageable pageable = PageRequest.of(0, 10);
		when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
		when(rankingKeyManger.getWeeklyRankingKey()).thenReturn("ranking:weekly");
		when(rankingKeyManger.getMonthlyRankingKey()).thenReturn("ranking:monthly");
		when(zSetOperations.zCard("ranking:weekly")).thenReturn(0L);
		when(zSetOperations.zCard("ranking:monthly")).thenReturn(0L);

		Page<RankingItem> weekly = repository.fetchWeekly(pageable);
		Page<RankingItem> monthly = repository.fetchMonthly(pageable);

		assertThat(weekly).isEmpty();
		assertThat(monthly).isEmpty();
		verify(rankingKeyManger).getWeeklyRankingKey();
		verify(rankingKeyManger).getMonthlyRankingKey();
	}

	@Test
	@DisplayName("daily rank가 없으면 null을 반환한다")
	void fetchDailyRank_returnsNullWhenRankMissing() {
		LocalDate date = LocalDate.of(2026, 3, 1);
		String key = "ranking:daily:20260301";

		when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
		when(rankingKeyManger.getDailyRankingKey(date)).thenReturn(key);
		when(zSetOperations.reverseRank(key, "10")).thenReturn(null);

		Long result = repository.fetchDailyRank(10L, date);

		assertThat(result).isNull();
	}

	@Test
	@DisplayName("daily rank는 Redis 0-base 값을 1-base로 변환한다")
	void fetchDailyRank_returnsOneBasedRank() {
		LocalDate date = LocalDate.of(2026, 3, 1);
		String key = "ranking:daily:20260301";

		when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
		when(rankingKeyManger.getDailyRankingKey(date)).thenReturn(key);
		when(zSetOperations.reverseRank(key, "10")).thenReturn(2L);

		Long result = repository.fetchDailyRank(10L, date);

		assertThat(result).isEqualTo(3L);
	}
}
