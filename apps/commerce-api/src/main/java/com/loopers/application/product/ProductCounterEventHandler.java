package com.loopers.application.product;

import java.util.function.Supplier;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.application.ranking.RankingRealtimeUpdateService;
import com.loopers.domain.product.ProductAggregateService;
import com.loopers.domain.product.ProductCacheInvalidationService;
import com.loopers.domain.product.ProductCounterType;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.product.event.ProductOrderedEvent;
import com.loopers.domain.product.event.ProductViewedEvent;
import com.loopers.support.event.Envelope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductCounterEventHandler {

	private final ProductCounterEventHistoryService productCounterEventHistoryService;
	private final ProductAggregateService productAggregateService;
	private final ProductCacheInvalidationService productCacheInvalidationService;
	private final RankingRealtimeUpdateService rankingRealtimeUpdateService;

	@Async("eventTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void handleProductOrdered(final Envelope<ProductOrderedEvent> event) {
		if (!ProductOrderedEvent.EVENT_TYPE.equals(event.getEventType())) {
			return;
		}

		ProductOrderedEvent payload = event.getPayload();
		ProductId productId = ProductId.of(payload.productId());
		String dedupeKey = productCounterEventHistoryService.generateDedupeKey(
			payload.getEventType() + "|" + payload.orderId() + "|" + payload.productId());

		if (!productCounterEventHistoryService.claim(dedupeKey, productId, ProductCounterType.ORDER)) {
			log.debug("중복 주문 카운터 이벤트를 무시합니다. dedupeKey={}, orderId={}, productId={}", dedupeKey,
				payload.orderId(), payload.productId());
			return;
		}

		try {
			productCounterEventHistoryService.markProcessing(dedupeKey);
			boolean success = incrementWithCreateIfMissing(productId,
				() -> productAggregateService.incrementOrderCount(productId));
			if (!success) {
				productCounterEventHistoryService.fail(dedupeKey, "주문 수 카운터 업데이트 실패");
				return;
			}

			productCounterEventHistoryService.complete(dedupeKey);
			productCacheInvalidationService.evictProductCache(productId);
			applyOrderScoreSafely(payload);
		} catch (Exception e) {
			productCounterEventHistoryService.fail(dedupeKey, e.getMessage());
			log.error("주문 카운터 이벤트 처리 실패. orderId={}, productId={}, dedupeKey={}",
				payload.orderId(), payload.productId(), dedupeKey, e);
		}
	}

	@Async("eventTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void handleProductViewed(final Envelope<ProductViewedEvent> event) {
		if (!ProductViewedEvent.EVENT_TYPE.equals(event.getEventType())) {
			return;
		}

		ProductViewedEvent payload = event.getPayload();
		ProductId productId = ProductId.of(payload.productId());
		String dedupeKey = productCounterEventHistoryService.generateDedupeKey(
			payload.getEventType() + "|" + payload.productId() + "|" + payload.occurredAt());

		if (!productCounterEventHistoryService.claim(dedupeKey, productId, ProductCounterType.VIEW)) {
			log.debug("중복 조회 카운터 이벤트를 무시합니다. dedupeKey={}, productId={}", dedupeKey, payload.productId());
			return;
		}

		try {
			productCounterEventHistoryService.markProcessing(dedupeKey);
			boolean success = incrementWithCreateIfMissing(productId,
				() -> productAggregateService.incrementViewCount(productId));
			if (!success) {
				productCounterEventHistoryService.fail(dedupeKey, "조회 수 카운터 업데이트 실패");
				return;
			}

			productCounterEventHistoryService.complete(dedupeKey);
			productCacheInvalidationService.evictProductCache(productId);
			applyViewScoreSafely(payload);
		} catch (Exception e) {
			productCounterEventHistoryService.fail(dedupeKey, e.getMessage());
			log.error("조회 카운터 이벤트 처리 실패. productId={}, dedupeKey={}", payload.productId(), dedupeKey, e);
		}
	}

	private boolean incrementWithCreateIfMissing(final ProductId productId, final Supplier<Boolean> incrementAction) {
		boolean success = incrementAction.get();
		if (success) {
			return true;
		}

		productAggregateService.createIfNotExists(productId);
		return incrementAction.get();
	}

	private void applyOrderScoreSafely(final ProductOrderedEvent payload) {
		try {
			rankingRealtimeUpdateService.incrementOrder(payload.productId(), payload.occurredAt());
		} catch (Exception e) {
			log.warn("주문 이벤트 랭킹 점수 업데이트에 실패했습니다. orderId={}, productId={}",
				payload.orderId(), payload.productId(), e);
		}
	}

	private void applyViewScoreSafely(final ProductViewedEvent payload) {
		try {
			rankingRealtimeUpdateService.incrementView(payload.productId(), payload.occurredAt());
		} catch (Exception e) {
			log.warn("조회 이벤트 랭킹 점수 업데이트에 실패했습니다. productId={}", payload.productId(), e);
		}
	}
}
