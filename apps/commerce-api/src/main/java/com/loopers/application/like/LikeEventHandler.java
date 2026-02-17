package com.loopers.application.like;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.domain.product.ProductId;
import com.loopers.domain.product.ProductAggregateService;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.event.ProductLikedEvent;
import com.loopers.domain.product.event.ProductUnLikedEvent;
import com.loopers.support.event.Envelope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class LikeEventHandler {

	private final ProductService productService;
	private final ProductAggregateService productAggregateService;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleProductLiked(Envelope<ProductLikedEvent> event) {
		if (!ProductLikedEvent.EVENT_TYPE.equals(event.getEventType())) {
			return;
		}
		ProductLikedEvent productLikedEvent = event.getPayload();
		ProductId productId = ProductId.of(productLikedEvent.productId());

		// Update Query로 원자적 처리
		boolean success = productAggregateService.incrementLikeCount(productId);

		if (!success) {
			log.warn("좋아요 수 업데이트 실패 - 상품 없음: {}", productLikedEvent.productId());
			productAggregateService.createIfNotExists(productId);
			productAggregateService.incrementLikeCount(productId);
		}

		productService.evictProductRelatedCaches(productId);
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleProductUnliked(Envelope<ProductUnLikedEvent> event) {
		if (!ProductUnLikedEvent.EVENT_TYPE.equals(event.getEventType())) {
			return;
		}
		ProductUnLikedEvent productUnLikedEvent = event.getPayload();
		ProductId productId = ProductId.of(productUnLikedEvent.productId());

		boolean success = productAggregateService.decrementLikeCount(productId);

		if (!success) {
			log.warn("좋아요 취소 업데이트 실패: productId={}", productUnLikedEvent.productId());
			return;
		}

		productService.evictProductRelatedCaches(productId);
	}
}
