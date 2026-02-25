package com.loopers.interfaces.scheduler.product;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.loopers.application.product.ProductCounterReconciliationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCounterReconciliationScheduler {

	private static final int FAILED_RETRY_BATCH_SIZE = 200;

	private final ProductCounterReconciliationService productCounterReconciliationService;

	@Scheduled(fixedDelay = 300000)
	public void reconcileProductCounters() {
		productCounterReconciliationService.reconcileAllAggregates();
	}

	@Scheduled(fixedDelay = 120000)
	public void retryFailedCounterEvents() {
		productCounterReconciliationService.retryFailedCounterEvents(FAILED_RETRY_BATCH_SIZE);
	}
}
