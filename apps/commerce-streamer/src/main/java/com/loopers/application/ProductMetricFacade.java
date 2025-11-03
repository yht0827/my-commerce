package com.loopers.application;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.config.event.ProductAggregationEvent;
import com.loopers.domain.metrics.ProductMetricData;
import com.loopers.domain.metrics.ProductMetricService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMetricFacade {

	private final ProductMetricProcessor productMetricProcessor;
	private final ProductMetricService productMetricService;

	@Transactional
	public void handleLikeChangedEvents(List<ProductAggregationEvent> events) {

		// Kafka 이벤트를 도메인 DTO로 변환
		List<ProductMetricData> metricDataList = productMetricProcessor.parseEvents(events);

		// ProductLikeAggregationEvent 배치 처리
		productMetricService.processProductMetrics(metricDataList);
	}
}
