package com.loopers.domain.metrics;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.loopers.config.kafka.KafkaGroups;
import com.loopers.domain.event.EventHandled;
import com.loopers.domain.event.EventHandledRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMetricService {

	private final ProductMetricRepository productMetricRepository;
	private final EventHandledRepository eventHandledRepository;

	public void processProductMetrics(List<ProductMetricData> metricDataList) {
		Map<Long, Long> productLikeChanges = new HashMap<>();
		List<EventHandled> handledEvents = new ArrayList<>();

		for (ProductMetricData data : metricDataList) {
			try {
				// 중복/버전 체크
				if (eventHandledRepository.isNewerVersion(data.eventId(), KafkaGroups.Metrics,
					data.version(), data.occurredAt())) {
					log.debug("이미 처리된 또는 오래된 메트릭 이벤트 - EventId: {}, Version: {}, OccurredAt: {}",
						data.eventId(), data.version(), data.occurredAt());
					continue;
				}

				// 좋아요 변화 집계
				Long currentChange = productLikeChanges.getOrDefault(data.productId(), 0L);
				productLikeChanges.put(data.productId(), currentChange + data.getLikeChange());

				// 처리된 이벤트 기록
				EventHandled eventHandled = createEventHandled(data);
				handledEvents.add(eventHandled);

			} catch (Exception e) {
				log.error("메트릭 처리 실패 - EventId: {}, ProductId: {}",
					data.eventId(), data.productId(), e);
			}
		}

		// 배치 저장
		saveProductMetrics(productLikeChanges, handledEvents);
	}

	public EventHandled createEventHandled(ProductMetricData data) {
		return EventHandled.builder()
			.eventId(data.eventId())
			.consumerGroup(KafkaGroups.Metrics)
			.version(data.version())
			.lastProcessedAt(LocalDateTime.now())
			.build();
	}

	public void saveProductMetrics(Map<Long, Long> productLikeChanges, List<EventHandled> handledEvents) {
		if (!productLikeChanges.isEmpty()) {

			for (Map.Entry<Long, Long> entry : productLikeChanges.entrySet()) {
				Long productId = entry.getKey();
				Long likeChange = entry.getValue();

				ProductMetric metrics = productMetricRepository.findByProductId(productId)
					.orElse(ProductMetric.of(productId));

				metrics.updateLikeCount(likeChange);
				productMetricRepository.save(metrics);
			}

			eventHandledRepository.saveAll(handledEvents);

			log.info("메트릭 배치 저장 완료 - 총 {}개 상품 처리", productLikeChanges.size());
		} else {
			log.debug("처리할 새로운 메트릭이 없습니다");
		}
	}
}
