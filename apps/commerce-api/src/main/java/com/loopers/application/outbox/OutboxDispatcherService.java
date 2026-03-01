package com.loopers.application.outbox;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.loopers.application.payment.PaymentResultOutboxService;
import com.loopers.application.payment.PaymentProcessor;
import com.loopers.application.platform.DataPlatformApplicationService;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.OrderData;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventType;
import com.loopers.domain.outbox.OutboxService;
import com.loopers.domain.payment.PaymentInfo;
import com.loopers.domain.point.Balance;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.ProductCacheInvalidationService;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.product.ProductStockService;
import com.loopers.domain.product.event.ProductOrderedEvent;
import com.loopers.domain.user.UserId;
import com.loopers.support.event.EventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxDispatcherService {

	private static final int BATCH_SIZE = 100;

	private final OutboxService outboxService;
	private final OutboxPayloadMapper outboxPayloadMapper;
	private final PaymentProcessor paymentProcessor;
	private final PaymentResultOutboxService paymentResultOutboxService;
	private final OrderService orderService;
	private final DataPlatformApplicationService dataPlatformApplicationService;
	private final EventPublisher eventPublisher;
	private final ProductStockService productStockService;
	private final ProductCacheInvalidationService productCacheInvalidationService;
	private final PointService pointService;
	private final CouponService couponService;

	public void dispatchPendingEvents() {
		// 스케줄러 1회 실행마다 처리 가능한 PENDING 이벤트를 배치 단위로 가져온다.
		List<Long> pendingEventIds = outboxService.findPendingEventIds(BATCH_SIZE);
		for (Long pendingEventId : pendingEventIds) {
			dispatchSingleEvent(pendingEventId);
		}
	}

	void dispatchSingleEvent(final Long outboxEventId) {
		Optional<OutboxEvent> optionalOutboxEvent = outboxService.claim(outboxEventId);
		if (optionalOutboxEvent.isEmpty()) {
			return;
		}

		OutboxEvent outboxEvent = optionalOutboxEvent.get();

		try {
			dispatchByType(outboxEvent);
			outboxService.complete(outboxEvent.getId());
		} catch (Exception e) {
			log.error("아웃박스 이벤트 처리 실패: id={}, eventType={}, aggregateId={}", outboxEvent.getId(),
				outboxEvent.getEventType(), outboxEvent.getAggregateId(), e);
			outboxService.retry(outboxEvent.getId(), e.getMessage());
		}
	}

	private void dispatchByType(final OutboxEvent outboxEvent) {
		OutboxEventType eventType = outboxEvent.getEventType();
		switch (eventType) {
			// 주문 커밋 이후 실제 결제 요청 실행
			case PAYMENT_REQUEST -> processPaymentRequest(outboxEvent.getPayload());
			// 결제 결과를 주문 상태(CONFIRMED/CANCELLED)로 동기화
			case ORDER_STATUS_SYNC -> processOrderStatusSync(outboxEvent.getPayload());
			// 데이터 플랫폼 전송(실패 시 retry)
			case DATA_PLATFORM_DISPATCH -> processDataPlatformDispatch(outboxEvent.getPayload());
		}
	}

	private void processPaymentRequest(final String payload) {
		OutboxPayload.PaymentRequest requestPayload = outboxPayloadMapper.read(payload, OutboxPayload.PaymentRequest.class);
		PaymentInfo paymentInfo = paymentProcessor.process(requestPayload.toCommand());
		paymentResultOutboxService.enqueueByPaymentStatus(paymentInfo.orderId(), paymentInfo.status());
	}

	private void processOrderStatusSync(final String payload) {
		OutboxPayload.OrderStatusSync orderStatusSync = outboxPayloadMapper.read(payload, OutboxPayload.OrderStatusSync.class);
		orderService.updateOrderStatus(orderStatusSync.orderId(), orderStatusSync.status());

		if (orderStatusSync.status() == OrderStatus.CONFIRMED) {
			publishProductOrderedEvents(orderStatusSync.orderId());
		}

		if (orderStatusSync.status() == OrderStatus.CANCELLED) {
			restoreOnCancelled(orderStatusSync.orderId());
		}
	}

	private void restoreOnCancelled(final String orderId) {
		restoreStockAndInvalidateCache(orderId);
		restorePointAndCoupon(orderId);
	}

	private void restoreStockAndInvalidateCache(final String orderId) {
		List<OrderItem> orderItems = orderService.getOrderItems(orderId);
		if (orderItems.isEmpty()) {
			log.warn("주문 상품 정보가 없어 재고 복구를 건너뜁니다. orderId={}", orderId);
			return;
		}

		productStockService.restoreStock(orderItems);

		Set<Long> productIds = new LinkedHashSet<>();
		for (OrderItem orderItem : orderItems) {
			productIds.add(orderItem.getProductId().getProductId());
		}
		for (Long productId : productIds) {
			productCacheInvalidationService.evictProductRelatedCaches(ProductId.of(productId));
		}
	}

	private void restorePointAndCoupon(final String orderId) {
		OrderData.CompensationInfo compensationInfo = orderService.getCompensationInfo(orderId);

		pointService.refund(
			UserId.of(compensationInfo.userId()),
			Balance.of(BigDecimal.valueOf(compensationInfo.finalPaymentAmount()))
		);

		if (compensationInfo.couponId() != null) {
			couponService.restoreCoupon(compensationInfo.couponId());
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

	private void processDataPlatformDispatch(final String payload) {
		OutboxPayload.DataPlatformDispatch dataPlatformDispatch = outboxPayloadMapper.read(payload,
			OutboxPayload.DataPlatformDispatch.class);
		dataPlatformApplicationService.sendEventWithFailure(dataPlatformDispatch.toEvent());
	}
}
