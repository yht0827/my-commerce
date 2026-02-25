package com.loopers.application.ranking;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.loopers.support.ranking.RankingEventType;
import com.loopers.support.ranking.RankingKeyManger;
import com.loopers.support.ranking.RankingScoreCalculator;
import com.loopers.support.ranking.RankingType;

@Service
public class RankingRealtimeUpdateService {

	private final StringRedisTemplate redisTemplate;
	private final RankingKeyManger rankingKeyManger;
	private final RankingScoreCalculator rankingScoreCalculator;

	public RankingRealtimeUpdateService(
		final StringRedisTemplate redisTemplate,
		final RankingKeyManger rankingKeyManger,
		final RankingScoreCalculator rankingScoreCalculator
	) {
		this.redisTemplate = redisTemplate;
		this.rankingKeyManger = rankingKeyManger;
		this.rankingScoreCalculator = rankingScoreCalculator;
	}

	public void incrementLike(final Long productId, final LocalDateTime occurredAt) {
		applyDailyScore(productId, RankingEventType.LIKE, 1L, occurredAt);
	}

	public void decrementLike(final Long productId, final LocalDateTime occurredAt) {
		applyDailyScore(productId, RankingEventType.LIKE, -1L, occurredAt);
	}

	public void incrementView(final Long productId, final LocalDateTime occurredAt) {
		applyDailyScore(productId, RankingEventType.VIEW, 1L, occurredAt);
	}

	public void incrementOrder(final Long productId, final LocalDateTime occurredAt) {
		applyDailyScore(productId, RankingEventType.ORDER, 1L, occurredAt);
	}

	private void applyDailyScore(
		final Long productId,
		final RankingEventType eventType,
		final long deltaCount,
		final LocalDateTime occurredAt
	) {
		if (productId == null || occurredAt == null || deltaCount == 0L) {
			return;
		}

		String key = rankingKeyManger.getDailyRankingKey(occurredAt.toLocalDate());
		double scoreDelta = rankingScoreCalculator.calculateScore(eventType) * deltaCount;

		redisTemplate.opsForZSet().incrementScore(key, String.valueOf(productId), scoreDelta);
		redisTemplate.expire(key, Duration.ofSeconds(rankingKeyManger.getTTL(RankingType.DAILY)));
	}
}
