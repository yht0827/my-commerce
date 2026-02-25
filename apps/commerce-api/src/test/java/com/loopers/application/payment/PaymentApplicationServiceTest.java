package com.loopers.application.payment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.loopers.domain.order.event.OrderCreatedEvent;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.PaymentData;
import com.loopers.domain.payment.PaymentInfo;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.TransactionStatus;

@DisplayName("PaymentApplicationService 테스트")
class PaymentApplicationServiceTest {

	private final PaymentService paymentService = mock(PaymentService.class);
	private final PaymentProcessor paymentProcessor = mock(PaymentProcessor.class);
	private final PaymentCallbackSignatureVerifier paymentCallbackSignatureVerifier = mock(PaymentCallbackSignatureVerifier.class);
	private final PaymentCallbackHistoryService paymentCallbackHistoryService = mock(PaymentCallbackHistoryService.class);
	private final PaymentCallbackAsyncProcessor paymentCallbackAsyncProcessor = mock(PaymentCallbackAsyncProcessor.class);
	private final PaymentApplicationService paymentApplicationService = new PaymentApplicationService(
		paymentService,
		paymentProcessor,
		paymentCallbackSignatureVerifier,
		paymentCallbackHistoryService,
		paymentCallbackAsyncProcessor
	);

	@Test
	@DisplayName("콜백 접수 시 신규 멱등 키면 비동기 처리를 시작하고 true 를 반환한다")
	void acceptCallbackReturnsTrueWhenClaimed() {
		PaymentCommand.ProcessCallback command = new PaymentCommand.ProcessCallback(
			"20260101:TR:abc123",
			TransactionStatus.SUCCESS,
			"ORD-1001",
			"cb-1",
			"1739950043",
			"sig",
			"{\"transactionKey\":\"20260101:TR:abc123\",\"status\":\"SUCCESS\",\"orderId\":\"ORD-1001\"}"
		);

		when(paymentCallbackHistoryService.generateDedupeKey(command)).thenReturn("cb-1");
		when(paymentCallbackHistoryService.claim("cb-1", command)).thenReturn(true);

		boolean accepted = paymentApplicationService.acceptCallback(command);

		assertThat(accepted).isTrue();
		verify(paymentCallbackSignatureVerifier).verify(command);
		verify(paymentCallbackAsyncProcessor).process("cb-1", command);
	}

	@Test
	@DisplayName("콜백 접수 시 중복 멱등 키면 비동기 처리 없이 false 를 반환한다")
	void acceptCallbackReturnsFalseWhenDuplicated() {
		PaymentCommand.ProcessCallback command = new PaymentCommand.ProcessCallback(
			"20260101:TR:abc123",
			TransactionStatus.SUCCESS,
			"ORD-1001",
			"cb-dup",
			"1739950043",
			"sig",
			"{\"transactionKey\":\"20260101:TR:abc123\",\"status\":\"SUCCESS\",\"orderId\":\"ORD-1001\"}"
		);

		when(paymentCallbackHistoryService.generateDedupeKey(command)).thenReturn("cb-dup");
		when(paymentCallbackHistoryService.claim("cb-dup", command)).thenReturn(false);

		boolean accepted = paymentApplicationService.acceptCallback(command);

		assertThat(accepted).isFalse();
		verify(paymentCallbackSignatureVerifier).verify(command);
		verify(paymentCallbackAsyncProcessor, never()).process(anyString(), any());
	}

	@Test
	@DisplayName("콜백 처리 성공 시 Success 결과를 반환한다")
	void processCallbackReturnsSuccess() {
		PaymentCommand.ProcessCallback command = new PaymentCommand.ProcessCallback(
			"20260101:TR:abc123",
			TransactionStatus.SUCCESS,
			"ORD-1001",
			null,
			null,
			null,
			null
		);
		PaymentInfo paymentInfo = new PaymentInfo(
			"20260101:TR:abc123",
			"ORD-1001",
			CardType.SAMSUNG,
			"1234-5678-1234-5678",
			10000L,
			TransactionStatus.SUCCESS,
			"정상 승인"
		);

		when(paymentService.processCallback(any(PaymentData.ProcessCallback.class))).thenReturn(paymentInfo);

		PaymentResult result = paymentApplicationService.processCallback(command);

		assertThat(result).isInstanceOf(PaymentResult.Success.class);
		PaymentResult.Success success = (PaymentResult.Success)result;
		assertThat(success.orderId()).isEqualTo("ORD-1001");
		assertThat(success.transactionKey()).isEqualTo("20260101:TR:abc123");
		verify(paymentService).processCallback(any(PaymentData.ProcessCallback.class));
	}

	@Test
	@DisplayName("주문 이벤트에 결제 메타데이터가 없으면 Failure 결과를 반환한다")
	void processOrderPaymentReturnsFailureWhenMetadataMissing() {
		OrderCreatedEvent orderEvent = OrderCreatedEvent.create("user1234", "ORD-1001", 10000L, null);

		PaymentResult result = paymentApplicationService.processOrderPayment(orderEvent);

		assertThat(result).isInstanceOf(PaymentResult.Failure.class);
		verifyNoInteractions(paymentProcessor);
	}

	@Test
	@DisplayName("주문 이벤트 결제 처리 성공 시 Success 결과를 반환한다")
	void processOrderPaymentReturnsSuccess() {
		OrderCreatedEvent.PaymentMetadata metadata = OrderCreatedEvent.PaymentMetadata.of(
			CardType.SAMSUNG,
			"1234-5678-1234-5678",
			"https://callback.example.com/payments"
		);
		OrderCreatedEvent orderEvent = OrderCreatedEvent.create("user1234", "ORD-1001", 10000L, metadata);
		PaymentInfo paymentInfo = new PaymentInfo(
			"20260101:TR:abc123",
			"ORD-1001",
			CardType.SAMSUNG,
			"1234-5678-1234-5678",
			10000L,
			TransactionStatus.SUCCESS,
			"정상 승인"
		);

		when(paymentProcessor.process(any(PaymentCommand.CreatePayment.class))).thenReturn(paymentInfo);

		PaymentResult result = paymentApplicationService.processOrderPayment(orderEvent);

		assertThat(result).isInstanceOf(PaymentResult.Success.class);
		verify(paymentProcessor).process(any(PaymentCommand.CreatePayment.class));
	}
}
