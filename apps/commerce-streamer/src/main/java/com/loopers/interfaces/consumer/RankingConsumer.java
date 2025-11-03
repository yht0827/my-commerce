package com.loopers.interfaces.consumer;

import static com.loopers.config.kafka.KafkaConfig.*;
import static com.loopers.config.kafka.KafkaGroups.*;
import static com.loopers.config.kafka.KafkaTopics.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.loopers.config.event.ProductAggregationEvent;
import com.loopers.domain.metrics.RankingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class RankingConsumer {

	private final RankingService rankingService;

	@KafkaListener(topics = PRODUCT_LIKE, groupId = Ranking, containerFactory = BATCH_LISTENER)
	public void handleRankingEvent(List<ConsumerRecord<String, ProductAggregationEvent>> records,
		Acknowledgment acknowledgment) {

		if (records == null || records.isEmpty()) {
			acknowledgment.acknowledge();
			return;
		}

		// 이벤트 통계 로깅
		Map<String, Long> topicCounts = records.stream()
			.collect(Collectors.groupingBy(ConsumerRecord::topic, Collectors.counting()));

		log.debug("랭킹 이벤트 수신 - 토픽별 개수: {}", topicCounts);

		try {
			// 랭킹 점수 처리
			rankingService.handleAggregateScores(records);
			log.info("랭킹 배치 처리 완료 - 총 {}건", records.size());

		} catch (Exception e) {
			log.error("랭킹 배치 처리 실패 - 총 {}건", records.size(), e);
		} finally {
			// 성공,실패 관계없이 항상 acknowledge
			acknowledgment.acknowledge();
		}

	}

}
