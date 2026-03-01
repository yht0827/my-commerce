package com.loopers.domain.payment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("PaymentService 테스트")
class PaymentServiceTest {

	private PaymentRepository paymentRepository;
	private PaymentGatewayService paymentGatewayService;
	private PaymentService paymentService;

	@BeforeEach
	void setUp() {
		paymentRepository = mock(PaymentRepository.class);
		paymentGatewayService = mock(PaymentGatewayService.class);
		paymentService = new PaymentService(paymentRepository, paymentGatewayService);
	}

	@DisplayName("createPayment 메서드")
	@Nested
	class CreatePayment {

		@DisplayName("결제 정보를 저장하고 반환한다")
		@Test
		void savesAndReturnsPayment() {
			// arrange
			PaymentData.PaymentRequest request = new PaymentData.PaymentRequest(
				"user1234",
				"ORD-1001",
				CardType.KB,
				"1234-5678-1234-5678",
				10000L,
				"https://callback.example.com/payments"
			);
			Payment entity = request.toEntity();
			when(paymentRepository.save(any(Payment.class))).thenReturn(entity);

			// act
			Payment result = paymentService.createPayment(request);

			// assert
			assertThat(result).isNotNull();
			assertThat(result.getUserId().getUserId()).isEqualTo("user1234");
			assertThat(result.getStatus()).isEqualTo(TransactionStatus.PENDING);
			verify(paymentRepository).save(any(Payment.class));
		}
	}

	@DisplayName("updatePaymentStatus 메서드")
	@Nested
	class UpdatePaymentStatus {

		@DisplayName("PG 응답에 transactionKey 가 있으면 결제의 transactionKey 를 갱신한다")
		@Test
		void updatesTransactionKeyWhenPresent() {
			Payment payment = Payment.builder()
				.userId(new com.loopers.domain.user.UserId("user1234"))
				.orderId(new com.loopers.domain.order.OrderId("ORD-1001"))
				.cardType(CardType.KB)
				.cardNo(new CardNo("1234-5678-1234-5678"))
				.amount(new com.loopers.domain.common.Price(10000L))
				.status(TransactionStatus.PENDING)
				.reason(new PaymentReason("결제 요청"))
				.callbackUrl(new CallbackUrl("https://callback.example.com/payments"))
				.transactionKey(new TransactionKey("old-key"))
				.build();

			PaymentInfo.Transaction pgResponse = new PaymentInfo.Transaction(
				"new-txn-key", "ORD-1001", 10000L, "1234-5678-1234-5678",
				CardType.KB, TransactionStatus.SUCCESS, "정상 승인"
			);

			PaymentInfo result = paymentService.updatePaymentStatus(payment, pgResponse);

			assertThat(payment.getTransactionKey().getTransactionKey()).isEqualTo("new-txn-key");
			assertThat(result.status()).isEqualTo(TransactionStatus.SUCCESS);
		}

		@DisplayName("PG 응답에 transactionKey 가 null 이면 기존 transactionKey 를 유지한다")
		@Test
		void keepsOldTransactionKeyWhenPgResponseKeyIsNull() {
			Payment payment = Payment.builder()
				.userId(new com.loopers.domain.user.UserId("user1234"))
				.orderId(new com.loopers.domain.order.OrderId("ORD-1001"))
				.cardType(CardType.KB)
				.cardNo(new CardNo("1234-5678-1234-5678"))
				.amount(new com.loopers.domain.common.Price(10000L))
				.status(TransactionStatus.PENDING)
				.reason(new PaymentReason("결제 요청"))
				.callbackUrl(new CallbackUrl("https://callback.example.com/payments"))
				.transactionKey(new TransactionKey("old-key"))
				.build();

			PaymentInfo.Transaction pgResponse = new PaymentInfo.Transaction(
				null, "ORD-1001", 10000L, "1234-5678-1234-5678",
				CardType.KB, TransactionStatus.FAILED, "실패"
			);

			PaymentInfo result = paymentService.updatePaymentStatus(payment, pgResponse);

			assertThat(payment.getTransactionKey().getTransactionKey()).isEqualTo("old-key");
			assertThat(result.status()).isEqualTo(TransactionStatus.FAILED);
		}

		@DisplayName("PG 응답 status 가 null 이면 PENDING 으로 처리한다")
		@Test
		void defaultsToPendingWhenStatusIsNull() {
			Payment payment = Payment.builder()
				.userId(new com.loopers.domain.user.UserId("user1234"))
				.orderId(new com.loopers.domain.order.OrderId("ORD-1001"))
				.cardType(CardType.KB)
				.cardNo(new CardNo("1234-5678-1234-5678"))
				.amount(new com.loopers.domain.common.Price(10000L))
				.status(TransactionStatus.INITIAL)
				.reason(new PaymentReason("결제 요청"))
				.callbackUrl(new CallbackUrl("https://callback.example.com/payments"))
				.transactionKey(new TransactionKey("txn-key"))
				.build();

			PaymentInfo.Transaction pgResponse = new PaymentInfo.Transaction(
				null, "ORD-1001", 10000L, "1234-5678-1234-5678",
				CardType.KB, null, null
			);

			PaymentInfo result = paymentService.updatePaymentStatus(payment, pgResponse);

			assertThat(result.status()).isEqualTo(TransactionStatus.PENDING);
		}

		@DisplayName("PG 응답 status 가 INITIAL 이면 PENDING 으로 처리한다")
		@Test
		void processesPendingWhenStatusIsInitial() {
			Payment payment = Payment.builder()
				.userId(new com.loopers.domain.user.UserId("user1234"))
				.orderId(new com.loopers.domain.order.OrderId("ORD-1001"))
				.cardType(CardType.KB)
				.cardNo(new CardNo("1234-5678-1234-5678"))
				.amount(new com.loopers.domain.common.Price(10000L))
				.status(TransactionStatus.INITIAL)
				.reason(new PaymentReason("결제 요청"))
				.callbackUrl(new CallbackUrl("https://callback.example.com/payments"))
				.transactionKey(new TransactionKey("txn-key"))
				.build();

			PaymentInfo.Transaction pgResponse = new PaymentInfo.Transaction(
				null, "ORD-1001", 10000L, "1234-5678-1234-5678",
				CardType.KB, TransactionStatus.INITIAL, null
			);

			PaymentInfo result = paymentService.updatePaymentStatus(payment, pgResponse);

			assertThat(result.status()).isEqualTo(TransactionStatus.PENDING);
		}
	}

	@DisplayName("processCallback 메서드")
	@Nested
	class ProcessCallback {

		@DisplayName("transactionKey로 결제를 찾지 못하면 NOT_FOUND 예외가 발생한다")
		@Test
		void throwsNotFoundWhenPaymentNotFound() {
			// arrange
			PaymentData.ProcessCallback command = new PaymentData.ProcessCallback(
				"txn-key-999",
				TransactionStatus.SUCCESS,
				"ORD-1001"
			);
			when(paymentRepository.findByTransactionKeyForUpdate("txn-key-999")).thenReturn(Optional.empty());

			// act & assert
			assertThatThrownBy(() -> paymentService.processCallback(command))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
		}

		@DisplayName("결제를 찾으면 콜백을 처리하고 결과를 반환한다")
		@Test
		void processesCallbackAndReturnsResult() {
			// arrange
			PaymentData.ProcessCallback command = new PaymentData.ProcessCallback(
				"txn-key-001",
				TransactionStatus.SUCCESS,
				"ORD-1001"
			);

			Payment payment = Payment.builder()
				.userId(new com.loopers.domain.user.UserId("user1234"))
				.orderId(new com.loopers.domain.order.OrderId("ORD-1001"))
				.cardType(CardType.KB)
				.cardNo(new CardNo("1234-5678-1234-5678"))
				.amount(new com.loopers.domain.common.Price(10000L))
				.status(TransactionStatus.PENDING)
				.reason(new PaymentReason("결제 요청"))
				.callbackUrl(new CallbackUrl("https://callback.example.com/payments"))
				.transactionKey(new TransactionKey("txn-key-001"))
				.build();

			PaymentInfo expectedInfo = PaymentInfo.from(payment);

			when(paymentRepository.findByTransactionKeyForUpdate("txn-key-001")).thenReturn(Optional.of(payment));
			when(paymentGatewayService.processCallbackWithVerification(payment, command)).thenReturn(expectedInfo);

			// act
			PaymentInfo result = paymentService.processCallback(command);

			// assert
			assertThat(result).isNotNull();
			assertThat(result.orderId()).isEqualTo("ORD-1001");
			verify(paymentGatewayService).processCallbackWithVerification(payment, command);
		}
	}
}
