package com.loopers.application.product;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.domain.product.ProductId;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.event.ProductOutOfStockEvent;
import com.loopers.support.event.Envelope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductEventHandler {

	private final ProductService productService;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleOutOfStock(Envelope<ProductOutOfStockEvent> event) {
		if (!ProductOutOfStockEvent.EVENT_TYPE.equals(event.getEventType())) {
			return;
		}
		ProductOutOfStockEvent stockDepletedEvent = event.getPayload();
		try {
			productService.evictProductRelatedCaches(ProductId.of(stockDepletedEvent.productId()));
		} catch (Exception e) {
			log.error("품절 이벤트 처리 실패: productId={}", stockDepletedEvent.productId(), e);
		}
	}
}
