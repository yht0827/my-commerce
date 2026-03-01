package com.loopers.application.payment;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.loopers.application.outbox.OutboxPayloadMapper;
import com.loopers.domain.outbox.OutboxEventType;
import com.loopers.domain.outbox.OutboxService;
import com.loopers.domain.payment.TransactionStatus;

@DisplayName("PaymentResultOutboxService 테스트")
class PaymentResultOutboxServiceTest {

	private final OutboxService outboxService = mock(OutboxService.class);
	private final OutboxPayloadMapper outboxPayloadMapper = mock(OutboxPayloadMapper.class);
	private final PaymentResultOutboxService paymentResultOutboxService =
		new PaymentResultOutboxService(outboxService, outboxPayloadMapper);

	@Test
	@DisplayName("결제 성공이면 주문 상태 동기화와 데이터 플랫폼 전송 아웃박스를 적재한다")
	void enqueueByPaymentStatus_successEnqueuesTwoEvents() {
		when(outboxPayloadMapper.write(any())).thenReturn("{}");
		when(outboxService.enqueue(any(), anyString(), anyString(), anyString())).thenReturn(true);

		paymentResultOutboxService.enqueueByPaymentStatus("ORD-1", TransactionStatus.SUCCESS);

		verify(outboxService, times(1)).enqueue(
			eq(OutboxEventType.ORDER_STATUS_SYNC),
			eq("ORD-1"),
			eq("order-status-sync:ORD-1:CONFIRMED"),
			eq("{}")
		);
		verify(outboxService, times(1)).enqueue(
			eq(OutboxEventType.DATA_PLATFORM_DISPATCH),
			eq("ORD-1"),
			eq("data-platform:PAYMENT_COMPLETED:ORD-1"),
			eq("{}")
		);
	}

	@Test
	@DisplayName("결제 실패면 주문 취소 동기화와 데이터 플랫폼 전송 아웃박스를 적재한다")
	void enqueueByPaymentStatus_failedEnqueuesTwoEvents() {
		when(outboxPayloadMapper.write(any())).thenReturn("{}");
		when(outboxService.enqueue(any(), anyString(), anyString(), anyString())).thenReturn(true);

		paymentResultOutboxService.enqueueByPaymentStatus("ORD-1", TransactionStatus.FAILED);

		verify(outboxService, times(1)).enqueue(
			eq(OutboxEventType.ORDER_STATUS_SYNC),
			eq("ORD-1"),
			eq("order-status-sync:ORD-1:CANCELLED"),
			eq("{}")
		);
		verify(outboxService, times(1)).enqueue(
			eq(OutboxEventType.DATA_PLATFORM_DISPATCH),
			eq("ORD-1"),
			eq("data-platform:PAYMENT_FAILED:ORD-1"),
			eq("{}")
		);
	}

	@Test
	@DisplayName("결제가 PENDING 이면 아웃박스를 적재하지 않는다")
	void enqueueByPaymentStatus_pendingDoesNotEnqueue() {
		paymentResultOutboxService.enqueueByPaymentStatus("ORD-1", TransactionStatus.PENDING);

		verifyNoInteractions(outboxService, outboxPayloadMapper);
	}
}
