package com.loopers.infrastructure.rank;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import com.loopers.domain.rank.RankingItem;
import com.loopers.domain.rank.RedisRankingRepository;
import com.loopers.support.ranking.RankingKeyManger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRankingRepositoryImpl implements RedisRankingRepository {

	private final StringRedisTemplate redisTemplate;
	private final RankingKeyManger rankingKeyManger;

	@Override
	public Page<RankingItem> fetchDaily(LocalDate date, Pageable pageable) {
		String key = rankingKeyManger.getDailyRankingKey(date);

		Long total = redisTemplate.opsForZSet().zCard(key);
		if (total == null || total == 0) {
			return Page.empty(pageable);
		}

		int start = Math.toIntExact(pageable.getOffset());
		int end = start + pageable.getPageSize() - 1;

		Set<ZSetOperations.TypedTuple<String>> rankedProducts = redisTemplate.opsForZSet()
			.reverseRangeWithScores(key, start, end);

		if (start >= total || rankedProducts == null || rankedProducts.isEmpty()) {
			return new PageImpl<>(List.of(), pageable, total);
		}

		List<RankingItem> items = toItems(rankedProducts, start);
		return new PageImpl<>(items, pageable, total);
	}

	@Override
	public Long fetchRank(Long productId, LocalDate date) {
		String key = rankingKeyManger.getDailyRankingKey(date);
		Long rank = redisTemplate.opsForZSet().reverseRank(key, String.valueOf(productId));
		if (rank == null) {
			return null;
		}
		return rank + 1;
	}

	private List<RankingItem> toItems(Set<ZSetOperations.TypedTuple<String>> rankedProducts, int start) {
		List<RankingItem> items = new ArrayList<>(rankedProducts.size());
		long rankBase = start + 1L;

		for (ZSetOperations.TypedTuple<String> tuple : rankedProducts) {
			if (tuple == null || tuple.getValue() == null || tuple.getScore() == null) {
				continue;
			}

			Long productId;
			try {
				productId = Long.parseLong(tuple.getValue());
			} catch (NumberFormatException ex) {
				log.warn("Invalid productId in ranking: {}", tuple.getValue());
				continue;
			}

			items.add(RankingItem.create()
				.rank(rankBase++)
				.productId(productId)
				.score(tuple.getScore())
				.likeCount(null)
				.viewCount(null)
				.orderCount(null)
				.build());
		}

		return items;
	}
}
