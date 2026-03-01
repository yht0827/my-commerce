package com.loopers.application.outbox;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.loopers.application.payment.PaymentResultOutboxService;
import com.loopers.application.payment.PaymentProcessor;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.OrderData;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventType;
import com.loopers.domain.outbox.OutboxService;
import com.loopers.domain.outbox.OutboxStatus;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.PaymentInfo;
import com.loopers.domain.payment.TransactionStatus;
import com.loopers.application.platform.DataPlatformApplicationService;
import com.loopers.domain.common.Price;
import com.loopers.domain.common.Quantity;
import com.loopers.domain.order.OrderId;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.product.ProductCacheInvalidationService;
import com.loopers.domain.product.ProductStockService;
import com.loopers.domain.coupon.CouponService;
import com.loopers.support.event.EventPublisher;

@DisplayName("OutboxDispatcherService 테스트")
class OutboxDispatcherServiceTest {

	private final OutboxService outboxService = mock(OutboxService.class);
	private final OutboxPayloadMapper outboxPayloadMapper = mock(OutboxPayloadMapper.class);
	private final PaymentProcessor paymentProcessor = mock(PaymentProcessor.class);
	private final PaymentResultOutboxService paymentResultOutboxService = mock(PaymentResultOutboxService.class);
	private final OrderService orderService = mock(OrderService.class);
	private final DataPlatformApplicationService dataPlatformApplicationService = mock(DataPlatformApplicationService.class);
	private final EventPublisher eventPublisher = mock(EventPublisher.class);
	private final ProductStockService productStockService = mock(ProductStockService.class);
	private final ProductCacheInvalidationService productCacheInvalidationService = mock(ProductCacheInvalidationService.class);
	private final PointService pointService = mock(PointService.class);
	private final CouponService couponService = mock(CouponService.class);

	private final OutboxDispatcherService outboxDispatcherService = new OutboxDispatcherService(
		outboxService,
		outboxPayloadMapper,
		paymentProcessor,
		paymentResultOutboxService,
		orderService,
		dataPlatformApplicationService,
		eventPublisher,
		productStockService,
		productCacheInvalidationService,
		pointService,
		couponService
	);

	@Test
	@DisplayName("PAYMENT_REQUEST 이벤트를 처리하면 결제 처리 후 완료 상태로 마크한다")
	void dispatchSingleEvent_processesPaymentRequestAndCompletes() {
		OutboxEvent outboxEvent = outboxEvent(OutboxEventType.PAYMENT_REQUEST, "{payment}");
		OutboxPayload.PaymentRequest paymentRequest = new OutboxPayload.PaymentRequest(
			"user1",
			"ORD-1",
			9000L,
			CardType.KB,
			"1111-2222-3333-4444",
			"https://callback.test/orders"
		);

		when(outboxService.claim(1L)).thenReturn(Optional.of(outboxEvent));
		when(outboxPayloadMapper.read("{payment}", OutboxPayload.PaymentRequest.class)).thenReturn(paymentRequest);
		when(paymentProcessor.process(any())).thenReturn(new PaymentInfo(
			"TR-1", "ORD-1", CardType.KB, "1111-2222-3333-4444", 9000L, TransactionStatus.SUCCESS, "ok"
		));

		outboxDispatcherService.dispatchSingleEvent(1L);

		verify(paymentProcessor, times(1)).process(any());
		verify(paymentResultOutboxService, times(1)).enqueueByPaymentStatus("ORD-1", TransactionStatus.SUCCESS);
		verify(outboxService, times(1)).complete(outboxEvent.getId());
		verify(outboxService, never()).retry(anyLong(), anyString());
	}

	@Test
	@DisplayName("ORDER_STATUS_SYNC 이벤트를 처리하면 주문 상태를 반영한다")
	void dispatchSingleEvent_processesOrderStatusSync() {
		OutboxEvent outboxEvent = outboxEvent(OutboxEventType.ORDER_STATUS_SYNC, "{orderStatus}");
		OutboxPayload.OrderStatusSync orderStatusSync = new OutboxPayload.OrderStatusSync("ORD-1", OrderStatus.CONFIRMED);

		when(outboxService.claim(2L)).thenReturn(Optional.of(outboxEvent));
		when(outboxPayloadMapper.read("{orderStatus}", OutboxPayload.OrderStatusSync.class)).thenReturn(orderStatusSync);

		outboxDispatcherService.dispatchSingleEvent(2L);

		verify(orderService, times(1)).updateOrderStatus("ORD-1", OrderStatus.CONFIRMED);
		verify(outboxService, times(1)).complete(outboxEvent.getId());
		verify(outboxService, never()).retry(anyLong(), anyString());
	}

	@Test
	@DisplayName("DATA_PLATFORM_DISPATCH 처리 실패 시 재시도 상태로 마크한다")
	void dispatchSingleEvent_retriesWhenDataPlatformDispatchFails() {
		OutboxEvent outboxEvent = outboxEvent(OutboxEventType.DATA_PLATFORM_DISPATCH, "{dataPlatform}");
		OutboxPayload.DataPlatformDispatch dataPlatformDispatch = new OutboxPayload.DataPlatformDispatch("ORDER_CREATED", "ORD-1");

		when(outboxService.claim(3L)).thenReturn(Optional.of(outboxEvent));
		when(outboxPayloadMapper.read("{dataPlatform}", OutboxPayload.DataPlatformDispatch.class))
			.thenReturn(dataPlatformDispatch);
		doThrow(new RuntimeException("platform down"))
			.when(dataPlatformApplicationService).sendEventWithFailure(any());

		outboxDispatcherService.dispatchSingleEvent(3L);

		verify(outboxService, never()).complete(anyLong());
		verify(outboxService, times(1)).retry(eq(outboxEvent.getId()), contains("platform down"));
	}

	@Test
	@DisplayName("claim 결과가 비어있으면 아무 작업도 수행하지 않는다")
	void dispatchSingleEvent_doesNothingWhenClaimIsEmpty() {
		when(outboxService.claim(100L)).thenReturn(Optional.empty());

		outboxDispatcherService.dispatchSingleEvent(100L);

		verify(outboxService, never()).complete(anyLong());
		verify(outboxService, never()).retry(anyLong(), anyString());
		verifyNoInteractions(outboxPayloadMapper, paymentProcessor, paymentResultOutboxService, orderService,
			dataPlatformApplicationService, eventPublisher, productStockService, productCacheInvalidationService, pointService,
			couponService);
	}

	@Test
	@DisplayName("dispatchPendingEvents는 조회된 pending id를 순회해 dispatch한다")
	void dispatchPendingEvents_dispatchesAllPendingIds() {
		when(outboxService.findPendingEventIds(anyInt())).thenReturn(List.of(1L, 2L, 3L));
		when(outboxService.claim(anyLong())).thenReturn(Optional.empty());

		outboxDispatcherService.dispatchPendingEvents();

		verify(outboxService).findPendingEventIds(anyInt());
		verify(outboxService).claim(1L);
		verify(outboxService).claim(2L);
		verify(outboxService).claim(3L);
	}

	@Test
	@DisplayName("DATA_PLATFORM_DISPATCH 성공 시 complete 처리한다")
	void dispatchSingleEvent_completesWhenDataPlatformDispatchSucceeds() {
		OutboxEvent outboxEvent = outboxEvent(OutboxEventType.DATA_PLATFORM_DISPATCH, "{dataPlatform}");
		OutboxPayload.DataPlatformDispatch payload = new OutboxPayload.DataPlatformDispatch("ORDER_CREATED", "ORD-1");

		when(outboxService.claim(4L)).thenReturn(Optional.of(outboxEvent));
		when(outboxPayloadMapper.read("{dataPlatform}", OutboxPayload.DataPlatformDispatch.class)).thenReturn(payload);

		outboxDispatcherService.dispatchSingleEvent(4L);

		verify(dataPlatformApplicationService).sendEventWithFailure(any());
		verify(outboxService).complete(outboxEvent.getId());
		verify(outboxService, never()).retry(anyLong(), anyString());
	}

	@Test
	@DisplayName("ORDER_STATUS_SYNC CONFIRMED + 주문 상품 없음이면 이벤트 발행을 건너뛴다")
	void dispatchSingleEvent_confirmed_skipsPublishWhenNoOrderItems() {
		OutboxEvent outboxEvent = outboxEvent(OutboxEventType.ORDER_STATUS_SYNC, "{confirmed}");
		OutboxPayload.OrderStatusSync payload = new OutboxPayload.OrderStatusSync("ORD-1", OrderStatus.CONFIRMED);

		when(outboxService.claim(5L)).thenReturn(Optional.of(outboxEvent));
		when(outboxPayloadMapper.read("{confirmed}", OutboxPayload.OrderStatusSync.class)).thenReturn(payload);
		when(orderService.getOrderItems("ORD-1")).thenReturn(List.of());

		outboxDispatcherService.dispatchSingleEvent(5L);

		verify(orderService).updateOrderStatus("ORD-1", OrderStatus.CONFIRMED);
		verify(eventPublisher, never()).publish(any());
		verify(outboxService).complete(outboxEvent.getId());
	}

	@Test
	@DisplayName("ORDER_STATUS_SYNC CONFIRMED면 중복 상품을 제거해 PRODUCT_ORDERED 이벤트를 발행한다")
	void dispatchSingleEvent_confirmed_publishesUniqueProductEvents() {
		OutboxEvent outboxEvent = outboxEvent(OutboxEventType.ORDER_STATUS_SYNC, "{confirmed}");
		OutboxPayload.OrderStatusSync payload = new OutboxPayload.OrderStatusSync("ORD-1", OrderStatus.CONFIRMED);
		List<OrderItem> orderItems = List.of(
			orderItem("ORD-1", 10L, 1L),
			orderItem("ORD-1", 10L, 2L),
			orderItem("ORD-1", 20L, 1L)
		);

		when(outboxService.claim(6L)).thenReturn(Optional.of(outboxEvent));
		when(outboxPayloadMapper.read("{confirmed}", OutboxPayload.OrderStatusSync.class)).thenReturn(payload);
		when(orderService.getOrderItems("ORD-1")).thenReturn(orderItems);

		outboxDispatcherService.dispatchSingleEvent(6L);

		verify(eventPublisher, times(2)).publish(any());
		verify(outboxService).complete(outboxEvent.getId());
	}

	@Test
	@DisplayName("ORDER_STATUS_SYNC CANCELLED + 주문 상품 없음이면 재고 복구/캐시 무효화를 건너뛴다")
	void dispatchSingleEvent_cancelled_skipsStockRestoreWhenNoOrderItems() {
		OutboxEvent outboxEvent = outboxEvent(OutboxEventType.ORDER_STATUS_SYNC, "{cancelled-empty}");
		OutboxPayload.OrderStatusSync payload = new OutboxPayload.OrderStatusSync("ORD-1", OrderStatus.CANCELLED);
		OrderData.CompensationInfo compensationInfo = new OrderData.CompensationInfo("user1", 1000L, null);

		when(outboxService.claim(7L)).thenReturn(Optional.of(outboxEvent));
		when(outboxPayloadMapper.read("{cancelled-empty}", OutboxPayload.OrderStatusSync.class)).thenReturn(payload);
		when(orderService.getOrderItems("ORD-1")).thenReturn(List.of());
		when(orderService.getCompensationInfo("ORD-1")).thenReturn(compensationInfo);

		outboxDispatcherService.dispatchSingleEvent(7L);

		verify(productStockService, never()).restoreStock(anyList());
		verify(productCacheInvalidationService, never()).evictProductRelatedCaches(any());
		verify(pointService).refund(any(), any());
		verify(couponService, never()).restoreCoupon(anyLong());
		verify(outboxService).complete(outboxEvent.getId());
	}

	@Test
	@DisplayName("ORDER_STATUS_SYNC CANCELLED면 재고/포인트/쿠폰 복구를 수행한다")
	void dispatchSingleEvent_cancelled_restoresStockPointAndCoupon() {
		OutboxEvent outboxEvent = outboxEvent(OutboxEventType.ORDER_STATUS_SYNC, "{cancelled}");
		OutboxPayload.OrderStatusSync payload = new OutboxPayload.OrderStatusSync("ORD-1", OrderStatus.CANCELLED);
		List<OrderItem> orderItems = List.of(
			orderItem("ORD-1", 10L, 1L),
			orderItem("ORD-1", 20L, 2L),
			orderItem("ORD-1", 10L, 3L)
		);
		OrderData.CompensationInfo compensationInfo = new OrderData.CompensationInfo("user1", 15000L, 77L);

		when(outboxService.claim(8L)).thenReturn(Optional.of(outboxEvent));
		when(outboxPayloadMapper.read("{cancelled}", OutboxPayload.OrderStatusSync.class)).thenReturn(payload);
		when(orderService.getOrderItems("ORD-1")).thenReturn(orderItems);
		when(orderService.getCompensationInfo("ORD-1")).thenReturn(compensationInfo);

		outboxDispatcherService.dispatchSingleEvent(8L);

		verify(productStockService).restoreStock(orderItems);
		verify(productCacheInvalidationService).evictProductRelatedCaches(ProductId.of(10L));
		verify(productCacheInvalidationService).evictProductRelatedCaches(ProductId.of(20L));
		verify(pointService).refund(any(), any());
		verify(couponService).restoreCoupon(77L);
		verify(outboxService).complete(outboxEvent.getId());
		verify(outboxService, never()).retry(anyLong(), anyString());
	}

	private OutboxEvent outboxEvent(final OutboxEventType eventType, final String payload) {
		return OutboxEvent.create()
			.eventType(eventType)
			.aggregateId("ORD-1")
			.dedupeKey("dedupe-1")
			.payload(payload)
			.status(OutboxStatus.PENDING)
			.retryCount(0)
			.nextRetryAt(LocalDateTime.now().minusSeconds(1))
			.build();
	}

	private OrderItem orderItem(final String orderId, final Long productId, final Long quantity) {
		return OrderItem.builder()
			.orderId(new OrderId(orderId))
			.productId(ProductId.of(productId))
			.quantity(new Quantity(quantity))
			.price(new Price(1000L))
			.build();
	}
}
