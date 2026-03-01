package com.loopers.application.product;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.domain.product.ProductCacheInvalidationService;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.product.event.ProductQuantityChangedEvent;
import com.loopers.support.event.Envelope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductEventHandler {

	private final ProductCacheInvalidationService productCacheInvalidationService;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleQuantityChanged(Envelope<ProductQuantityChangedEvent> event) {
		if (!ProductQuantityChangedEvent.EVENT_TYPE.equals(event.getEventType())) {
			return;
		}

		ProductQuantityChangedEvent productQuantityChangedEvent = event.getPayload();

		try {
			ProductId productId = ProductId.of(productQuantityChangedEvent.productId());
			productCacheInvalidationService.evictProductRelatedCaches(productId);
		} catch (Exception e) {
			log.error(
				"상품 수량 변경 이벤트 처리 실패: productId={}, previousQuantity={}, currentQuantity={}",
				productQuantityChangedEvent.productId(),
				productQuantityChangedEvent.previousQuantity(),
				productQuantityChangedEvent.currentQuantity(),
				e
			);
		}
	}
}
