package com.loopers.domain.payment;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.loopers.domain.common.Price;
import com.loopers.domain.order.OrderId;
import com.loopers.domain.user.UserId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("Payment 엔티티 테스트")
class PaymentTest {

	@DisplayName("updateTransactionKey 메서드")
	@Nested
	class UpdateTransactionKey {

		@Test
		@DisplayName("orderId 가 null 이면 BAD_REQUEST 예외가 발생한다")
		void throwsWhenOrderIdIsNull() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");

			assertThatThrownBy(() -> payment.updateTransactionKey(null))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
		}

		@Test
		@DisplayName("orderId 가 빈 문자열이면 BAD_REQUEST 예외가 발생한다")
		void throwsWhenOrderIdIsBlank() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");

			assertThatThrownBy(() -> payment.updateTransactionKey("   "))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
		}

		@Test
		@DisplayName("유효한 orderId 로 transactionKey 를 갱신한다")
		void updatesTransactionKeySuccessfully() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");

			payment.updateTransactionKey("new-txn-key");

			assertThat(payment.getTransactionKey().getTransactionKey()).isEqualTo("new-txn-key");
		}
	}

	@DisplayName("updatePaymentStatus 메서드")
	@Nested
	class UpdatePaymentStatus {

		@Test
		@DisplayName("이미 SUCCESS 상태인 결제의 상태를 변경하면 BAD_REQUEST 예외가 발생한다")
		void throwsWhenAlreadySuccess() {
			Payment payment = paymentWith(TransactionStatus.SUCCESS, "txn-001", "ORD-1001");

			assertThatThrownBy(() -> payment.updatePaymentStatus(TransactionStatus.FAILED))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
		}

		@Test
		@DisplayName("PENDING 상태에서 SUCCESS 로 변경할 수 있다")
		void updatesFromPendingToSuccess() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");

			payment.updatePaymentStatus(TransactionStatus.SUCCESS);

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
		}

		@Test
		@DisplayName("PENDING 상태에서 FAILED 로 변경할 수 있다")
		void updatesFromPendingToFailed() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");

			payment.updatePaymentStatus(TransactionStatus.FAILED);

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.FAILED);
		}

		@Test
		@DisplayName("INITIAL 상태에서 PENDING 으로 변경할 수 있다")
		void updatesFromInitialToPending() {
			Payment payment = paymentWith(TransactionStatus.INITIAL, "txn-001", "ORD-1001");

			payment.updatePaymentStatus(TransactionStatus.PENDING);

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.PENDING);
		}

		@Test
		@DisplayName("FAILED 상태에서 다른 상태로 변경할 수 있다")
		void updatesFromFailedToPending() {
			Payment payment = paymentWith(TransactionStatus.FAILED, "txn-001", "ORD-1001");

			payment.updatePaymentStatus(TransactionStatus.PENDING);

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.PENDING);
		}
	}

	@DisplayName("processPaymentPending 메서드")
	@Nested
	class ProcessPaymentPending {

		@Test
		@DisplayName("결제 상태를 PENDING 으로 변경한다")
		void changesStatusToPending() {
			Payment payment = paymentWith(TransactionStatus.INITIAL, "txn-001", "ORD-1001");

			payment.processPaymentPending();

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.PENDING);
		}

		@Test
		@DisplayName("이미 SUCCESS 상태이면 예외가 발생한다")
		void throwsWhenAlreadySuccess() {
			Payment payment = paymentWith(TransactionStatus.SUCCESS, "txn-001", "ORD-1001");

			assertThatThrownBy(() -> payment.processPaymentPending())
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
		}
	}

	@DisplayName("processPaymentSuccess 메서드")
	@Nested
	class ProcessPaymentSuccess {

		@Test
		@DisplayName("결제 상태를 SUCCESS 로 변경한다")
		void changesStatusToSuccess() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");

			payment.processPaymentSuccess();

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
		}
	}

	@DisplayName("processPaymentFailed 메서드")
	@Nested
	class ProcessPaymentFailed {

		@Test
		@DisplayName("결제 상태를 FAILED 로 변경한다")
		void changesStatusToFailed() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");

			payment.processPaymentFailed();

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.FAILED);
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
