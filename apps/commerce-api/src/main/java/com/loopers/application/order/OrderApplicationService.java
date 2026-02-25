package com.loopers.application.order;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.application.outbox.OutboxPayload;
import com.loopers.application.outbox.OutboxPayloadMapper;
import com.loopers.domain.order.OrderData;
import com.loopers.domain.order.OrderHistory;
import com.loopers.domain.order.OrderHistoryRepository;
import com.loopers.domain.order.OrderInfo;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.event.OrderCreatedEvent;
import com.loopers.domain.outbox.OutboxEventType;
import com.loopers.domain.outbox.OutboxService;
import com.loopers.domain.product.ProductData;
import com.loopers.domain.product.event.ProductQuantityChangedEvent;
import com.loopers.support.error.CoreException;
import com.loopers.support.event.EventPublisher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderApplicationService {

	private final OrderService orderService;
	private final OrderHistoryRepository orderHistoryRepository;
	private final OrderProcessor orderProcessor;
	private final OutboxService outboxService;
	private final OutboxPayloadMapper outboxPayloadMapper;
	private final EventPublisher eventPublisher;

	@Transactional
	public OrderResult createOrder(final OrderCommand.CreateOrder command) {
		String idempotencyKey = normalizeIdempotencyKey(command.idempotencyKey());

		// 같은 사용자 + 같은 키의 중복 주문 요청은 기존 결과를 재사용한다.
		if (idempotencyKey != null && !claimOrderHistory(command.userId(), idempotencyKey)) {
			return recoverExistingOrder(command.userId(), idempotencyKey);
		}

		OrderResult createdOrder = createNewOrder(command);

		if (idempotencyKey != null) {
			completeOrderHistory(command.userId(), idempotencyKey, createdOrder.orderId());
		}

		return createdOrder;
	}

	private OrderResult createNewOrder(final OrderCommand.CreateOrder command) {
		// 주문 핵심 처리(재고/쿠폰/포인트/주문 저장)는 하나의 트랜잭션 안에서 동기 처리한다.
		OrderProcessResult processResult = orderProcessor.process(command);
		publishQuantityChangedEvents(processResult.quantityChanges());

		OrderInfo orderInfo = processResult.orderInfo();
		// 외부 I/O(결제 요청, 데이터 플랫폼 전송)는 커밋 후 outbox dispatcher가 처리한다.
		enqueuePaymentRequestOutbox(command, orderInfo, processResult.finalPaymentAmount());
		enqueueOrderCreatedDataPlatformOutbox(orderInfo.orderId());

		return OrderResult.from(orderInfo);
	}

	private void enqueuePaymentRequestOutbox(
		final OrderCommand.CreateOrder command,
		final OrderInfo orderInfo,
		final Long finalPaymentAmount
	) {
		// dedupeKey를 고정해 스케줄러 재처리/중복 요청에도 결제 요청 이벤트가 1회만 적재되게 한다.
		OutboxPayload.PaymentRequest payload = new OutboxPayload.PaymentRequest(
			command.userId(),
			orderInfo.orderId(),
			finalPaymentAmount,
			command.cardType(),
			command.cardNo(),
			command.callbackUrl()
		);

		outboxService.enqueue(
			OutboxEventType.PAYMENT_REQUEST,
			orderInfo.orderId(),
			"payment-request:%s".formatted(orderInfo.orderId()),
			outboxPayloadMapper.write(payload)
		);
	}

	private void enqueueOrderCreatedDataPlatformOutbox(final String orderId) {
		OutboxPayload.DataPlatformDispatch payload = new OutboxPayload.DataPlatformDispatch(
			OrderCreatedEvent.EVENT_TYPE,
			orderId
		);

		outboxService.enqueue(
			OutboxEventType.DATA_PLATFORM_DISPATCH,
			orderId,
			"data-platform:%s:%s".formatted(OrderCreatedEvent.EVENT_TYPE, orderId),
			outboxPayloadMapper.write(payload)
		);
	}

	private OrderResult recoverExistingOrder(final String userId, final String idempotencyKey) {
		OrderHistory orderHistory = findByUserIdAndIdempotencyKey(userId, idempotencyKey)
			.orElseThrow(() -> new CoreException(CONFLICT, ORDER_IDEMPOTENCY_KEY_IN_PROGRESS.format()));

		if (!orderHistory.isCompleted()) {
			throw new CoreException(CONFLICT, ORDER_IDEMPOTENCY_KEY_IN_PROGRESS.format());
		}

		OrderInfo orderInfo = orderService.getOrder(new OrderData.GetOrder(userId, orderHistory.getOrderId()));
		return OrderResult.from(orderInfo);
	}

	private boolean claimOrderHistory(final String userId, final String idempotencyKey) {
		return orderHistoryRepository.createIfNotExists(userId, idempotencyKey);
	}

	private java.util.Optional<OrderHistory> findByUserIdAndIdempotencyKey(
		final String userId,
		final String idempotencyKey
	) {
		return orderHistoryRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
	}

	private void completeOrderHistory(final String userId, final String idempotencyKey, final String orderId) {
		OrderHistory orderHistory = findByUserIdAndIdempotencyKey(userId, idempotencyKey)
			.orElseThrow(() -> new CoreException(NOT_FOUND, "멱등성 요청 이력을 찾을 수 없습니다."));
		orderHistory.complete(orderId);
	}

	private void publishQuantityChangedEvents(final List<ProductData.StockQuantityChanged> quantityChanges) {
		for (ProductData.StockQuantityChanged quantityChange : quantityChanges) {
			eventPublisher.publish(ProductQuantityChangedEvent.create(
				quantityChange.productId(),
				quantityChange.previousQuantity(),
				quantityChange.currentQuantity()
			));
		}
	}

	public List<OrderResult> getOrders(final OrderQuery.GetOrders command) {
		OrderData.GetOrders orderData = command.toData();
		List<OrderInfo> orderList = orderService.getOrders(orderData);
		return orderList.stream().map(OrderResult::from).toList();
	}

	public OrderResult getOrder(final OrderQuery.GetOrder command) {
		OrderData.GetOrder orderData = command.toData();
		OrderInfo orderInfo = orderService.getOrder(orderData);
		return OrderResult.from(orderInfo);
	}

	private String normalizeIdempotencyKey(final String idempotencyKey) {
		if (idempotencyKey == null) {
			return null;
		}

		String normalized = idempotencyKey.trim();
		return normalized.isBlank() ? null : normalized;
	}

}
