package com.loopers.application.order;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.event.OrderCreatedEvent;
import com.loopers.domain.product.event.ProductOrderedEvent;
import com.loopers.domain.payment.event.PaymentCompletedEvent;
import com.loopers.domain.payment.event.PaymentFailedEvent;
import com.loopers.domain.platform.event.DataPlatformEvent;
import com.loopers.support.event.Envelope;
import com.loopers.support.event.EventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventHandler {

	private final OrderService orderService;
	private final EventPublisher eventPublisher;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void processPaymentCompleted(Envelope<PaymentCompletedEvent> event) {
		if (!PaymentCompletedEvent.EVENT_TYPE.equals(event.getEventType())) {
			return;
		}
		PaymentCompletedEvent paymentCompletedEvent = event.getPayload();
		try {
			orderService.updateOrderStatus(paymentCompletedEvent.getOrderId(), OrderStatus.CONFIRMED);
			publishProductOrderedEvents(paymentCompletedEvent.orderId());
			log.info("주문 상태 업데이트 완료: {} -> CONFIRMED", paymentCompletedEvent.orderId());
		} catch (Exception e) {
			log.error("결제 완료 후 주문 상태 업데이트 실패: 주문 ID {}", paymentCompletedEvent.orderId(), e);
		}
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void processPaymentFailed(Envelope<PaymentFailedEvent> event) {
		if (!PaymentFailedEvent.EVENT_TYPE.equals(event.getEventType())) {
			return;
		}
		PaymentFailedEvent paymentFailedEvent = event.getPayload();
		try {
			orderService.updateOrderStatus(paymentFailedEvent.getOrderId(), OrderStatus.CANCELLED);
			log.info("주문 상태 업데이트 완료: {} -> CANCELLED", paymentFailedEvent.getOrderId());
		} catch (Exception e) {
			log.error("결제 실패 후 주문 상태 업데이트 실패: 주문 ID {}", paymentFailedEvent.getOrderId(), e);
		}
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleOrderCreatedForDataPlatform(Envelope<OrderCreatedEvent> event) {
		if (!OrderCreatedEvent.EVENT_TYPE.equals(event.getEventType())) {
			return;
		}
		OrderCreatedEvent orderEvent = event.getPayload();
		try {
			DataPlatformEvent dataPlatformEvent = DataPlatformEvent.create(orderEvent.getEventType(), orderEvent.orderId());
			eventPublisher.publish(dataPlatformEvent);
			log.info("주문 생성 이벤트를 데이터 플랫폼 이벤트로 발행: {}", orderEvent.orderId());
		} catch (Exception e) {
			log.error("데이터 플랫폼 이벤트 발행 실패: 주문 ID {}", orderEvent.orderId(), e);
		}
	}

	private void publishProductOrderedEvents(final String orderId) {
		List<OrderItem> orderItems = orderService.getOrderItems(orderId);
		if (orderItems.isEmpty()) {
			log.warn("주문 상품 정보가 없어 PRODUCT_ORDERED 이벤트 발행을 건너뜁니다. orderId={}", orderId);
			return;
		}

		Set<Long> productIds = new LinkedHashSet<>();
		for (OrderItem orderItem : orderItems) {
			productIds.add(orderItem.getProductId().getProductId());
		}

		for (Long productId : productIds) {
			eventPublisher.publish(ProductOrderedEvent.create(orderId, productId));
		}
	}
}
