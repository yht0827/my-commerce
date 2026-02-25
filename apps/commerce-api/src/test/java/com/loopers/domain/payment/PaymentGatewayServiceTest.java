package com.loopers.domain.payment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.loopers.domain.common.Price;
import com.loopers.domain.order.OrderId;
import com.loopers.domain.user.UserId;

@DisplayName("PaymentGatewayService 테스트")
class PaymentGatewayServiceTest {

	private final PgClient pgClient = mock(PgClient.class);
	private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
	private final PaymentStatusSynchronizer paymentStatusSynchronizer = mock(PaymentStatusSynchronizer.class);
	private final PaymentGatewayService paymentGatewayService = new PaymentGatewayService(
		pgClient,
		paymentRepository,
		paymentStatusSynchronizer
	);

	@Test
	@DisplayName("PG 조회 성공 시 교차 검증 경로를 사용한다")
	void processCallbackWithVerificationUsesPgDataOnSuccess() {
		Payment payment = paymentWith(TransactionStatus.PENDING, "20260101:TR:abc123", "ORD-1001");
		PaymentData.ProcessCallback callback = new PaymentData.ProcessCallback(
			"20260101:TR:abc123",
			TransactionStatus.SUCCESS,
			"ORD-1001"
		);
		PaymentInfo.Transaction pgTransaction = new PaymentInfo.Transaction(
			"20260101:TR:abc123",
			"ORD-1001",
			10000L,
			"1234-5678-1234-5678",
			CardType.SAMSUNG,
			TransactionStatus.SUCCESS,
			"정상 승인"
		);

		when(pgClient.findTransaction("20260101:TR:abc123")).thenReturn(pgTransaction);

		PaymentInfo result = paymentGatewayService.processCallbackWithVerification(payment, callback);

		assertThat(result.orderId()).isEqualTo("ORD-1001");
		verify(paymentStatusSynchronizer).processCallback(payment, callback, pgTransaction);
		verify(paymentStatusSynchronizer, never()).processCallbackWithException(any(), any());
	}

	@Test
	@DisplayName("PG 인프라 예외면 콜백 데이터 기반 예외 경로를 사용한다")
	void processCallbackWithVerificationUsesCallbackWhenPgClientException() {
		Payment payment = paymentWith(TransactionStatus.PENDING, "20260101:TR:abc123", "ORD-1001");
		PaymentData.ProcessCallback callback = new PaymentData.ProcessCallback(
			"20260101:TR:abc123",
			TransactionStatus.SUCCESS,
			"ORD-1001"
		);

		when(pgClient.findTransaction("20260101:TR:abc123"))
			.thenThrow(new PgClientException("pg timeout", new RuntimeException("timeout")));

		PaymentInfo result = paymentGatewayService.processCallbackWithVerification(payment, callback);

		assertThat(result.orderId()).isEqualTo("ORD-1001");
		verify(paymentStatusSynchronizer).processCallbackWithException(payment, callback);
		verify(paymentStatusSynchronizer, never()).processCallback(any(), any(), any());
	}

	@Test
	@DisplayName("예상하지 못한 예외는 상위로 전파한다")
	void processCallbackWithVerificationPropagatesUnexpectedException() {
		Payment payment = paymentWith(TransactionStatus.PENDING, "20260101:TR:abc123", "ORD-1001");
		PaymentData.ProcessCallback callback = new PaymentData.ProcessCallback(
			"20260101:TR:abc123",
			TransactionStatus.SUCCESS,
			"ORD-1001"
		);

		when(pgClient.findTransaction("20260101:TR:abc123"))
			.thenThrow(new IllegalStateException("unexpected"));

		assertThatThrownBy(() -> paymentGatewayService.processCallbackWithVerification(payment, callback))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("unexpected");

		verify(paymentStatusSynchronizer, never()).processCallbackWithException(any(), any());
	}

	private Payment paymentWith(TransactionStatus status, String transactionKey, String orderId) {
		return Payment.builder()
			.orderId(new OrderId(orderId))
			.userId(new UserId("user1234"))
			.cardType(CardType.SAMSUNG)
			.cardNo(new CardNo("1234-5678-1234-5678"))
			.amount(new Price(10000L))
			.callbackUrl(new CallbackUrl("https://callback.example.com/payments"))
			.transactionKey(new TransactionKey(transactionKey))
			.status(status)
			.reason(new PaymentReason("결제 요청"))
			.build();
	}
}
