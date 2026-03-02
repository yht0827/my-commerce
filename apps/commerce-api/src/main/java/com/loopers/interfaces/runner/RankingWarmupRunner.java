package com.loopers.interfaces.runner;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.loopers.domain.ranking.RankingItem;
import com.loopers.domain.ranking.RankingSnapshotRepository;
import com.loopers.support.ranking.RankingKeyManger;
import com.loopers.support.ranking.RankingType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingWarmupRunner implements ApplicationRunner {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final RankingSnapshotRepository rankingSnapshotRepository;
	private final RankingKeyManger rankingKeyManger;
	private final StringRedisTemplate rankingStringRedisTemplate;

	@Override
	public void run(ApplicationArguments args) {
		LocalDate today = LocalDate.now(KST);
		warmupDaily(today);
		warmupWeekly(today);
		warmupMonthly(today);
	}

	private void warmupDaily(LocalDate today) {
		try {
			String key = rankingKeyManger.getDailyRankingKey(today);
			Long size = rankingStringRedisTemplate.opsForZSet().size(key);
			if (size != null && size > 0) {
				log.info("[RankingWarmup] Daily ranking already populated (key={}, size={}), skipping.", key, size);
				return;
			}

			Page<RankingItem> snapshot = rankingSnapshotRepository.fetchDaily(today, Pageable.unpaged());
			if (snapshot.isEmpty()) {
				log.warn("[RankingWarmup] Daily ranking snapshot is empty for date={}, skipping.", today);
				return;
			}

			for (RankingItem item : snapshot) {
				rankingStringRedisTemplate.opsForZSet().add(key, String.valueOf(item.getProductId()), item.getScore());
			}
			rankingStringRedisTemplate.expire(key, Duration.ofSeconds(rankingKeyManger.getTTL(RankingType.DAILY)));
			log.info("[RankingWarmup] Daily ranking warmed up (key={}, count={}).", key, snapshot.getTotalElements());
		} catch (Exception e) {
			log.warn("[RankingWarmup] Failed to warmup daily ranking: {}", e.getMessage(), e);
		}
	}

	private void warmupWeekly(LocalDate today) {
		try {
			String key = rankingKeyManger.getWeeklyRankingKey();
			Long size = rankingStringRedisTemplate.opsForZSet().size(key);
			if (size != null && size > 0) {
				log.info("[RankingWarmup] Weekly ranking already populated (key={}, size={}), skipping.", key, size);
				return;
			}

			Page<RankingItem> snapshot = rankingSnapshotRepository.fetchWeekly(today, Pageable.unpaged());
			if (snapshot.isEmpty()) {
				log.warn("[RankingWarmup] Weekly ranking snapshot is empty, skipping.");
				return;
			}

			for (RankingItem item : snapshot) {
				rankingStringRedisTemplate.opsForZSet().add(key, String.valueOf(item.getProductId()), item.getScore());
			}
			rankingStringRedisTemplate.expire(key, Duration.ofSeconds(rankingKeyManger.getTTL(RankingType.WEEKLY)));
			log.info("[RankingWarmup] Weekly ranking warmed up (key={}, count={}).", key, snapshot.getTotalElements());
		} catch (Exception e) {
			log.warn("[RankingWarmup] Failed to warmup weekly ranking: {}", e.getMessage(), e);
		}
	}

	private void warmupMonthly(LocalDate today) {
		try {
			String key = rankingKeyManger.getMonthlyRankingKey();
			Long size = rankingStringRedisTemplate.opsForZSet().size(key);
			if (size != null && size > 0) {
				log.info("[RankingWarmup] Monthly ranking already populated (key={}, size={}), skipping.", key, size);
				return;
			}

			Page<RankingItem> snapshot = rankingSnapshotRepository.fetchMonthly(today, Pageable.unpaged());
			if (snapshot.isEmpty()) {
				log.warn("[RankingWarmup] Monthly ranking snapshot is empty, skipping.");
				return;
			}

			for (RankingItem item : snapshot) {
				rankingStringRedisTemplate.opsForZSet().add(key, String.valueOf(item.getProductId()), item.getScore());
			}
			rankingStringRedisTemplate.expire(key, Duration.ofSeconds(rankingKeyManger.getTTL(RankingType.MONTHLY)));
			log.info("[RankingWarmup] Monthly ranking warmed up (key={}, count={}).", key, snapshot.getTotalElements());
		} catch (Exception e) {
			log.warn("[RankingWarmup] Failed to warmup monthly ranking: {}", e.getMessage(), e);
		}
	}
}
