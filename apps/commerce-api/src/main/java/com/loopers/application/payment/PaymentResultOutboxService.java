package com.loopers.application.payment;

import org.springframework.stereotype.Service;

import com.loopers.application.outbox.OutboxPayload;
import com.loopers.application.outbox.OutboxPayloadMapper;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.outbox.OutboxEventType;
import com.loopers.domain.outbox.OutboxService;
import com.loopers.domain.payment.TransactionStatus;
import com.loopers.domain.payment.event.PaymentCompletedEvent;
import com.loopers.domain.payment.event.PaymentFailedEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentResultOutboxService {

	private final OutboxService outboxService;
	private final OutboxPayloadMapper outboxPayloadMapper;

	public void enqueueByPaymentStatus(final String orderId, final TransactionStatus status) {
		// 결제 성공은 주문 확정 + 데이터 플랫폼 성공 이벤트를 후속 처리한다.
		if (status == TransactionStatus.SUCCESS) {
			enqueueOrderStatusSync(orderId, OrderStatus.CONFIRMED);
			enqueueDataPlatformDispatch(PaymentCompletedEvent.EVENT_TYPE, orderId);
			return;
		}

		// 결제 실패는 주문 취소 + 데이터 플랫폼 실패 이벤트를 후속 처리한다.
		if (status == TransactionStatus.FAILED) {
			enqueueOrderStatusSync(orderId, OrderStatus.CANCELLED);
			enqueueDataPlatformDispatch(PaymentFailedEvent.EVENT_TYPE, orderId);
		}
	}

	private void enqueueOrderStatusSync(final String orderId, final OrderStatus targetStatus) {
		OutboxPayload.OrderStatusSync payload = new OutboxPayload.OrderStatusSync(orderId, targetStatus);
		outboxService.enqueue(
			OutboxEventType.ORDER_STATUS_SYNC,
			orderId,
			"order-status-sync:%s:%s".formatted(orderId, targetStatus.name()),
			outboxPayloadMapper.write(payload)
		);
	}

	private void enqueueDataPlatformDispatch(final String dataType, final String aggregateId) {
		OutboxPayload.DataPlatformDispatch payload = new OutboxPayload.DataPlatformDispatch(dataType, aggregateId);
		outboxService.enqueue(
			OutboxEventType.DATA_PLATFORM_DISPATCH,
			aggregateId,
			"data-platform:%s:%s".formatted(dataType, aggregateId),
			outboxPayloadMapper.write(payload)
		);
	}
}
