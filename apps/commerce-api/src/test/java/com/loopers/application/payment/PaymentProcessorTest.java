package com.loopers.application.payment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentGatewayService;
import com.loopers.domain.payment.PaymentInfo;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.TransactionStatus;

@DisplayName("PaymentProcessor 테스트")
class PaymentProcessorTest {

	private final PaymentService paymentService = mock(PaymentService.class);
	private final PaymentGatewayService paymentGatewayService = mock(PaymentGatewayService.class);
	private final PaymentProcessor paymentProcessor = new PaymentProcessor(paymentService, paymentGatewayService);

	@Test
	@DisplayName("결제 요청 처리 성공 시 PG 요청 결과를 반영한 결제 정보를 반환한다")
	void processReturnsUpdatedPaymentInfo() {
		PaymentCommand.CreatePayment command = new PaymentCommand.CreatePayment(
			"user1234",
			"ORD-1001",
			10000L,
			CardType.SAMSUNG,
			"1234-5678-1234-5678",
			"https://callback.example.com/payments"
		);

		Payment payment = mock(Payment.class);
		PaymentInfo.Transaction pgResponse = new PaymentInfo.Transaction(
			"20260101:TR:abc123",
			"ORD-1001",
			10000L,
			"1234-5678-1234-5678",
			CardType.SAMSUNG,
			TransactionStatus.SUCCESS,
			"정상 승인"
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

		when(paymentService.createPayment(any())).thenReturn(payment);
		when(paymentGatewayService.requestPaymentGateway(any())).thenReturn(pgResponse);
		when(paymentService.updatePaymentStatus(eq(payment), eq(pgResponse))).thenReturn(paymentInfo);

		PaymentInfo result = paymentProcessor.process(command);

		assertThat(result).isEqualTo(paymentInfo);
		verify(paymentService).createPayment(any());
		verify(paymentGatewayService).requestPaymentGateway(any());
		verify(paymentService).updatePaymentStatus(eq(payment), eq(pgResponse));
	}

	@Test
	@DisplayName("PG 요청 실패 시 예외를 그대로 전파한다")
	void processThrowsWhenPgRequestFails() {
		PaymentCommand.CreatePayment command = new PaymentCommand.CreatePayment(
			"user1234",
			"ORD-1001",
			10000L,
			CardType.SAMSUNG,
			"1234-5678-1234-5678",
			"https://callback.example.com/payments"
		);

		Payment payment = mock(Payment.class);
		when(paymentService.createPayment(any())).thenReturn(payment);
		when(paymentGatewayService.requestPaymentGateway(any())).thenThrow(new RuntimeException("pg error"));

		assertThatThrownBy(() -> paymentProcessor.process(command))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("pg error");

		verify(paymentService).createPayment(any());
		verify(paymentGatewayService).requestPaymentGateway(any());
		verify(paymentService, never()).updatePaymentStatus(any(), any());
	}
}
