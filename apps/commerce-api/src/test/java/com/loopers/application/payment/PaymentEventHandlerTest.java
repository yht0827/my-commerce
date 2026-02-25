package com.loopers.application.payment;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.loopers.domain.order.event.OrderCreatedEvent;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.PaymentInfo;
import com.loopers.domain.payment.TransactionStatus;
import com.loopers.support.event.EventPublisher;

@DisplayName("PaymentEventHandler 테스트")
class PaymentEventHandlerTest {

	private final PaymentApplicationService paymentApplicationService = mock(PaymentApplicationService.class);
	private final EventPublisher eventPublisher = mock(EventPublisher.class);
	private final PaymentEventHandler paymentEventHandler = new PaymentEventHandler(paymentApplicationService, eventPublisher);

	@Test
	@DisplayName("결제 결과가 SUCCESS 가 아니면 완료 이벤트를 발행하지 않는다")
	void handlePaymentResultSkipsCompletedEventWhenStatusNotSuccess() {
		PaymentInfo pendingInfo = new PaymentInfo(
			"20260101:TR:abc123",
			"ORD-1001",
			CardType.SAMSUNG,
			"1234-5678-1234-5678",
			10000L,
			TransactionStatus.PENDING,
			"처리중"
		);

		paymentEventHandler.handlePaymentResult(PaymentResult.success(pendingInfo));

		verifyNoInteractions(eventPublisher);
	}

	@Test
	@DisplayName("결제 처리 실패 결과는 실패 이벤트를 발행한다")
	void handlePaymentResultPublishesFailedEventWhenFailure() {
		OrderCreatedEvent.PaymentMetadata metadata = OrderCreatedEvent.PaymentMetadata.of(
			CardType.SAMSUNG,
			"1234-5678-1234-5678",
			"https://callback.example.com/payments"
		);
		OrderCreatedEvent orderEvent = OrderCreatedEvent.create("user1234", "ORD-1001", 10000L, metadata);

		paymentEventHandler.handlePaymentResult(PaymentResult.failure(orderEvent, new RuntimeException("pg error")));

		verify(eventPublisher, times(2)).publish(any());
	}
}
