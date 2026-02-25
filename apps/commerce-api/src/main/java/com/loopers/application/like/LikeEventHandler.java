package com.loopers.application.like;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.application.product.ProductCounterEventHistoryService;
import com.loopers.application.ranking.RankingRealtimeUpdateService;
import com.loopers.domain.product.ProductAggregateService;
import com.loopers.domain.product.ProductCacheInvalidationService;
import com.loopers.domain.product.ProductCounterType;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.product.event.ProductLikedEvent;
import com.loopers.domain.product.event.ProductUnLikedEvent;
import com.loopers.support.event.Envelope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class LikeEventHandler {

	private final ProductCacheInvalidationService productCacheInvalidationService;
	private final ProductAggregateService productAggregateService;
	private final ProductCounterEventHistoryService productCounterEventHistoryService;
	private final RankingRealtimeUpdateService rankingRealtimeUpdateService;

	@Async("eventTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleProductLiked(Envelope<ProductLikedEvent> event) {
		if (!ProductLikedEvent.EVENT_TYPE.equals(event.getEventType())) {
			return;
		}
		ProductLikedEvent productLikedEvent = event.getPayload();
		ProductId productId = ProductId.of(productLikedEvent.productId());
		String dedupeKey = productCounterEventHistoryService.generateDedupeKey(
			productLikedEvent.getEventType()
				+ "|" + productLikedEvent.userId()
				+ "|" + productLikedEvent.productId()
				+ "|" + productLikedEvent.occurredAt());

		if (!productCounterEventHistoryService.claim(dedupeKey, productId, ProductCounterType.LIKE)) {
			log.debug("중복 좋아요 이벤트를 무시합니다. dedupeKey={}, productId={}", dedupeKey, productLikedEvent.productId());
			return;
		}

		try {
			productCounterEventHistoryService.markProcessing(dedupeKey);

			boolean success = productAggregateService.incrementLikeCount(productId);

			if (!success) {
				log.warn("좋아요 수 업데이트 실패 - 상품 없음: {}", productLikedEvent.productId());
				productAggregateService.createIfNotExists(productId);
				success = productAggregateService.incrementLikeCount(productId);

				if (!success) {
					log.warn("좋아요 수 업데이트 재시도 실패: productId={}", productLikedEvent.productId());
					productCounterEventHistoryService.fail(dedupeKey, "좋아요 수 카운터 업데이트 실패");
					return;
				}
			}

			productCounterEventHistoryService.complete(dedupeKey);
			productCacheInvalidationService.evictProductRelatedCaches(productId);
			applyRankingDeltaSafely(productLikedEvent.productId(), productLikedEvent.occurredAt(), true);
		} catch (Exception e) {
			productCounterEventHistoryService.fail(dedupeKey, e.getMessage());
			log.error("좋아요 이벤트 처리 실패. productId={}, dedupeKey={}", productLikedEvent.productId(), dedupeKey, e);
		}
	}

	@Async("eventTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleProductUnliked(Envelope<ProductUnLikedEvent> event) {
		if (!ProductUnLikedEvent.EVENT_TYPE.equals(event.getEventType())) {
			return;
		}
		ProductUnLikedEvent productUnLikedEvent = event.getPayload();
		ProductId productId = ProductId.of(productUnLikedEvent.productId());
		String dedupeKey = productCounterEventHistoryService.generateDedupeKey(
			productUnLikedEvent.getEventType()
				+ "|" + productUnLikedEvent.userId()
				+ "|" + productUnLikedEvent.productId()
				+ "|" + productUnLikedEvent.occurredAt());

		if (!productCounterEventHistoryService.claim(dedupeKey, productId, ProductCounterType.LIKE)) {
			log.debug("중복 좋아요 취소 이벤트를 무시합니다. dedupeKey={}, productId={}", dedupeKey,
				productUnLikedEvent.productId());
			return;
		}

		try {
			productCounterEventHistoryService.markProcessing(dedupeKey);

			boolean success = productAggregateService.decrementLikeCount(productId);

			if (!success) {
				log.warn("좋아요 취소 업데이트 실패: productId={}", productUnLikedEvent.productId());
				productCounterEventHistoryService.fail(dedupeKey, "좋아요 취소 카운터 업데이트 실패");
				return;
			}

			productCounterEventHistoryService.complete(dedupeKey);
			productCacheInvalidationService.evictProductRelatedCaches(productId);
			applyRankingDeltaSafely(productUnLikedEvent.productId(), productUnLikedEvent.occurredAt(), false);
		} catch (Exception e) {
			productCounterEventHistoryService.fail(dedupeKey, e.getMessage());
			log.error("좋아요 취소 이벤트 처리 실패. productId={}, dedupeKey={}", productUnLikedEvent.productId(), dedupeKey, e);
		}
	}

	private void applyRankingDeltaSafely(
		final Long productId,
		final LocalDateTime occurredAt,
		final boolean increment
	) {
		try {
			if (increment) {
				rankingRealtimeUpdateService.incrementLike(productId, occurredAt);
				return;
			}

			rankingRealtimeUpdateService.decrementLike(productId, occurredAt);
		} catch (Exception e) {
			log.warn("랭킹 점수 업데이트에 실패했습니다. productId={}, increment={}", productId, increment, e);
		}
	}
}
