package com.loopers.domain.metrics;

import static com.loopers.support.ranking.RankingType.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.loopers.config.event.ProductAggregationEvent;
import com.loopers.support.RankingEventMapper;
import com.loopers.support.ranking.RankingEventType;
import com.loopers.support.ranking.RankingKeyManger;
import com.loopers.support.ranking.RankingScoreCalculator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingService {

	private final StringRedisTemplate redisTemplate;
	private final RankingKeyManger rankingKeyManger;
	private final RankingScoreCalculator scoreCalculator;
	private final ProductMetricsDailyRepository productMetricsDailyRepository;

	public void handleAggregateScores(List<ConsumerRecord<String, ProductAggregationEvent>> records) {
		if (records == null || records.isEmpty()) {
			return;
		}

		try {
			// 1. 이벤트를 날짜별, 상품별로 그룹화
			Map<LocalDate, Map<Long, List<ProductAggregationEvent>>> eventsByDateAndProduct = groupEventsByDateAndProduct(
				records);

			// 2. 이벤트 배치 업데이트
			for (Map.Entry<LocalDate, Map<Long, List<ProductAggregationEvent>>> dateEntry : eventsByDateAndProduct.entrySet()) {
				LocalDate date = dateEntry.getKey();
				Map<Long, List<ProductAggregationEvent>> productEvents = dateEntry.getValue();

				updateDailyRanking(date, productEvents);
			}

			log.debug("랭킹 점수 업데이트 완료 - 날짜별 그룹: {}", eventsByDateAndProduct.size());

		} catch (Exception e) {
			log.error("랭킹 점수 집계 처리 실패", e);
			throw e;
		}
	}

	public Map<LocalDate, Map<Long, List<ProductAggregationEvent>>> groupEventsByDateAndProduct(
		List<ConsumerRecord<String, ProductAggregationEvent>> records) {

		return records.stream()
			.filter(record -> record.value() != null)
			.map(ConsumerRecord::value)
			.filter(event -> event.productId() != null && event.eventDate() != null)
			.collect(Collectors.groupingBy(ProductAggregationEvent::eventDate, // 날짜별 그룹핑
				Collectors.groupingBy(ProductAggregationEvent::productId) // 상품별 그룹핑
			));
	}

	public void updateDailyRanking(LocalDate date, Map<Long, List<ProductAggregationEvent>> productEvents) {
		String rankingKey = rankingKeyManger.getDailyRankingKey(date);

		Map<Long, Double> scoreDelta = new HashMap<>();
		Map<Long, Long> likeDelta = new HashMap<>();

		// 델타 계산
		for (Map.Entry<Long, List<ProductAggregationEvent>> entry : productEvents.entrySet()) {
			Long productId = entry.getKey();
			List<ProductAggregationEvent> events = entry.getValue();

			// 점수 변화량 계산
			double score = calculateBatchEventScore(events);
			if (score != 0.0) {
				scoreDelta.put(productId, score);
			}

			// 좋아요 변화량 계산
			long like = events.stream()
				.mapToLong(ev -> "LIKE".equals(ev.action()) ? 1L :
					("UNLIKE".equals(ev.action()) ? -1L : 0L))
				.sum();
			if (like != 0L) {
				likeDelta.put(productId, like);
			}
		}

		// Redis 갱신
		redisTemplate.executePipelined((RedisCallback<Object>)connection -> {
			StringRedisConnection stringConn = (StringRedisConnection)connection;

			scoreDelta.forEach((pid, delta) -> {
				double finalScore = applyTieBreaker(delta, pid);
				stringConn.zIncrBy(rankingKey, finalScore, String.valueOf(pid));
			});

			// TTL 설정
			stringConn.expire(rankingKey, rankingKeyManger.getTTL(DAILY));
			return null;
		});

		log.debug("파이프라인 배치 업데이트 완료 - rankingKey: {}, products: {}", rankingKey, productEvents.size());

		//  DB 업데이트 대상 집합
		Set<Long> ids = new HashSet<>();
		ids.addAll(scoreDelta.keySet());
		ids.addAll(likeDelta.keySet());

		if (!ids.isEmpty()) {
			// 기존 데이터 조회
			List<ProductMetricsDaily> existing =
				productMetricsDailyRepository.findAllBySnapshotDateAndProductIdIn(date, ids);

			Map<Long, ProductMetricsDaily> existingMap = existing.stream()
				.collect(Collectors.toMap(ProductMetricsDaily::getProductId, e -> e));

			// upsert 대상 생성
			List<ProductMetricsDaily> productMetricsList = new ArrayList<>(ids.size());
			for (Long pid : ids) {
				ProductMetricsDaily metrics = existingMap.getOrDefault(pid, ProductMetricsDaily.create(pid, date));

				long like = likeDelta.getOrDefault(pid, 0L);
				double score = scoreDelta.getOrDefault(pid, 0.0);

				if (like != 0L)
					metrics.addLike(like);
				if (score != 0.0)
					metrics.addScore(score);

				productMetricsList.add(metrics);
			}

			// DB 병합
			productMetricsDailyRepository.saveAll(productMetricsList);
		}
	}

	public double calculateEventScore(ProductAggregationEvent event) {
		if (event == null || event.eventType() == null || event.occurredAt() == null) {
			return 0.0;
		}

		try {

			// 이벤트 타입에 따른 기본 점수 계산
			RankingEventType rankingEventType = RankingEventMapper.toRankingEventType(event.eventType());

			// 고정 가중치
			double baseScore = scoreCalculator.calculateScore(rankingEventType);

			// LIKE & UNLIKE  액션 배수
			double actionScore = baseScore * event.getScoreMultiplier();

			// 시간 감쇠 적용 (최근일수록 높은 점수)
			double finalScore = scoreCalculator.applyTimeDecay(actionScore, event.occurredAt());

			log.trace("랭킹 점수 계산 처리 성공 - eventType: {}, baseScore: {}, actionScore: {}, finalScore: {}", event.eventType(),
				baseScore, actionScore, finalScore);

			return finalScore;

		} catch (Exception e) {
			log.error("랭킹 점수 계산 처리 실패", e);
		}

		return 0.0;
	}

	public double calculateBatchEventScore(List<ProductAggregationEvent> events) {
		if (events == null || events.isEmpty()) {
			return 0.0;
		}

		// 유효하지 않은 이벤트 필터링 및 점수 계산을 한 번에 처리
		return events.stream()
			.filter(event -> event != null && event.eventType() != null && event.occurredAt() != null)
			.mapToDouble(this::calculateEventScore)
			.sum();
	}

	public double applyTieBreaker(double baseScore, Long productId) {
		return baseScore + (productId % 1000) * 1e-9;
	}

	public void carryOverRankingScores() {
		String todayKey = rankingKeyManger.getDailyRankingKey(LocalDate.now());
		String tomorrowKey = rankingKeyManger.getDailyRankingKey(LocalDate.now().plusDays(1));

		redisTemplate.opsForZSet().unionAndStore(todayKey, Collections.emptySet(), tomorrowKey);

		Duration ttl = Duration.ofSeconds(rankingKeyManger.getTTL(DAILY));
		redisTemplate.expire(tomorrowKey, ttl);

		log.info("랭킹 점수 이월 완료: {} -> {}", todayKey, tomorrowKey);
	}
}
