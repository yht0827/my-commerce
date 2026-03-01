package com.loopers.domain.payment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.loopers.domain.common.Price;
import com.loopers.domain.order.OrderId;
import com.loopers.domain.user.UserId;

@DisplayName("PaymentGatewayService 확장 테스트")
class PaymentGatewayServiceExtendedTest {

	private final PgClient pgClient = mock(PgClient.class);
	private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
	private final PaymentStatusSynchronizer paymentStatusSynchronizer = mock(PaymentStatusSynchronizer.class);
	private final PaymentGatewayService paymentGatewayService = new PaymentGatewayService(
		pgClient,
		paymentRepository,
		paymentStatusSynchronizer
	);

	@DisplayName("requestPaymentGateway 메서드")
	@Nested
	class RequestPaymentGateway {

		@Test
		@DisplayName("PG 클라이언트에 결제 요청을 위임하고 결과를 반환한다")
		void delegatesToPgClient() {
			PaymentData.PaymentRequest request = new PaymentData.PaymentRequest(
				"user1234", "ORD-1001", CardType.SAMSUNG,
				"1234-5678-1234-5678", 10000L, "https://callback.example.com/payments"
			);
			PaymentInfo.Transaction expected = new PaymentInfo.Transaction(
				"txn-001", "ORD-1001", 10000L, "1234-5678-1234-5678",
				CardType.SAMSUNG, TransactionStatus.SUCCESS, "정상 승인"
			);

			when(pgClient.request(request)).thenReturn(expected);

			PaymentInfo.Transaction result = paymentGatewayService.requestPaymentGateway(request);

			assertThat(result).isEqualTo(expected);
			verify(pgClient).request(request);
		}
	}

	@DisplayName("verifyPendingPayments 메서드")
	@Nested
	class VerifyPendingPayments {

		@Test
		@DisplayName("PENDING 결제가 없으면 빈 리스트를 반환한다")
		void returnsEmptyWhenNoPendingPayments() {
			when(paymentRepository.findByStatus(TransactionStatus.PENDING)).thenReturn(List.of());

			List<PaymentInfo> result = paymentGatewayService.verifyPendingPayments();

			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("PG 조회 결과로 상태가 SUCCESS 로 전이된 결제를 반환한다")
		void returnsTerminalTransitionPayments() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");

			when(paymentRepository.findByStatus(TransactionStatus.PENDING)).thenReturn(List.of(payment));
			PaymentInfo.Order pgOrder = new PaymentInfo.Order("ORD-1001", List.of(
				new PaymentInfo.TransactionResponse("txn-001", TransactionStatus.SUCCESS, "정상 승인")
			));
			when(pgClient.findOrder("ORD-1001")).thenReturn(pgOrder);
			doAnswer(invocation -> {
				Payment p = invocation.getArgument(0);
				p.processPaymentSuccess();
				return null;
			}).when(paymentStatusSynchronizer).processScheduler(payment, TransactionStatus.SUCCESS);

			List<PaymentInfo> result = paymentGatewayService.verifyPendingPayments();

			assertThat(result).hasSize(1);
			assertThat(result.getFirst().status()).isEqualTo(TransactionStatus.SUCCESS);
		}

		@Test
		@DisplayName("PG 응답이 null 이면 상태를 변경하지 않고 빈 리스트를 반환한다")
		void returnsEmptyWhenPgResponseIsNull() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");

			when(paymentRepository.findByStatus(TransactionStatus.PENDING)).thenReturn(List.of(payment));
			when(pgClient.findOrder("ORD-1001")).thenReturn(null);

			List<PaymentInfo> result = paymentGatewayService.verifyPendingPayments();

			assertThat(result).isEmpty();
			verify(paymentStatusSynchronizer, never()).processScheduler(any(), any());
		}

		@Test
		@DisplayName("PG 응답의 transactions 가 null 이면 상태를 변경하지 않는다")
		void returnsEmptyWhenPgTransactionsNull() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");

			when(paymentRepository.findByStatus(TransactionStatus.PENDING)).thenReturn(List.of(payment));
			when(pgClient.findOrder("ORD-1001")).thenReturn(new PaymentInfo.Order("ORD-1001", null));

			List<PaymentInfo> result = paymentGatewayService.verifyPendingPayments();

			assertThat(result).isEmpty();
			verify(paymentStatusSynchronizer, never()).processScheduler(any(), any());
		}

		@Test
		@DisplayName("PG 응답의 transactions 가 비어있으면 상태를 변경하지 않는다")
		void returnsEmptyWhenPgTransactionsEmpty() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");

			when(paymentRepository.findByStatus(TransactionStatus.PENDING)).thenReturn(List.of(payment));
			when(pgClient.findOrder("ORD-1001")).thenReturn(new PaymentInfo.Order("ORD-1001", List.of()));

			List<PaymentInfo> result = paymentGatewayService.verifyPendingPayments();

			assertThat(result).isEmpty();
			verify(paymentStatusSynchronizer, never()).processScheduler(any(), any());
		}

		@Test
		@DisplayName("PG 조회 중 예외 발생 시 해당 건을 건너뛰고 나머지를 처리한다")
		void skipsPaymentWhenPgThrowsException() {
			Payment payment1 = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");
			Payment payment2 = paymentWith(TransactionStatus.PENDING, "txn-002", "ORD-1002");

			when(paymentRepository.findByStatus(TransactionStatus.PENDING)).thenReturn(List.of(payment1, payment2));
			when(pgClient.findOrder("ORD-1001")).thenThrow(new RuntimeException("PG 조회 실패"));
			PaymentInfo.Order pgOrder2 = new PaymentInfo.Order("ORD-1002", List.of(
				new PaymentInfo.TransactionResponse("txn-002", TransactionStatus.FAILED, "승인 거부")
			));
			when(pgClient.findOrder("ORD-1002")).thenReturn(pgOrder2);
			doAnswer(invocation -> {
				Payment p = invocation.getArgument(0);
				p.processPaymentFailed();
				return null;
			}).when(paymentStatusSynchronizer).processScheduler(payment2, TransactionStatus.FAILED);

			List<PaymentInfo> result = paymentGatewayService.verifyPendingPayments();

			assertThat(result).hasSize(1);
			assertThat(result.getFirst().status()).isEqualTo(TransactionStatus.FAILED);
		}

		@Test
		@DisplayName("상태 전이가 터미널이 아니면 결과에 포함하지 않는다")
		void doesNotIncludeNonTerminalTransition() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");

			when(paymentRepository.findByStatus(TransactionStatus.PENDING)).thenReturn(List.of(payment));
			PaymentInfo.Order pgOrder = new PaymentInfo.Order("ORD-1001", List.of(
				new PaymentInfo.TransactionResponse("txn-001", TransactionStatus.PENDING, "대기중")
			));
			when(pgClient.findOrder("ORD-1001")).thenReturn(pgOrder);

			List<PaymentInfo> result = paymentGatewayService.verifyPendingPayments();

			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("transactionKey 가 null 인 결제는 첫 번째 트랜잭션을 사용한다")
		void usesFirstTransactionWhenTransactionKeyIsNull() {
			Payment payment = Payment.builder()
				.orderId(new OrderId("ORD-1001"))
				.userId(new UserId("user1234"))
				.cardType(CardType.SAMSUNG)
				.cardNo(new CardNo("1234-5678-1234-5678"))
				.amount(new Price(10000L))
				.callbackUrl(new CallbackUrl("https://callback.example.com/payments"))
				.transactionKey(null)
				.status(TransactionStatus.PENDING)
				.reason(new PaymentReason("결제 요청"))
				.build();

			when(paymentRepository.findByStatus(TransactionStatus.PENDING)).thenReturn(List.of(payment));
			PaymentInfo.Order pgOrder = new PaymentInfo.Order("ORD-1001", List.of(
				new PaymentInfo.TransactionResponse("txn-first", TransactionStatus.SUCCESS, "정상 승인")
			));
			when(pgClient.findOrder("ORD-1001")).thenReturn(pgOrder);
			doAnswer(invocation -> {
				Payment p = invocation.getArgument(0);
				p.processPaymentSuccess();
				return null;
			}).when(paymentStatusSynchronizer).processScheduler(payment, TransactionStatus.SUCCESS);

			List<PaymentInfo> result = paymentGatewayService.verifyPendingPayments();

			assertThat(result).hasSize(1);
			verify(paymentStatusSynchronizer).processScheduler(payment, TransactionStatus.SUCCESS);
		}
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
