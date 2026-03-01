package com.loopers.domain.payment;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.loopers.domain.common.Price;
import com.loopers.domain.order.OrderId;
import com.loopers.domain.user.UserId;

@DisplayName("PaymentStatusSynchronizer 확장 테스트")
class PaymentStatusSynchronizerExtendedTest {

	private final PaymentStatusSynchronizer synchronizer = new PaymentStatusSynchronizer();

	@DisplayName("processScheduler 메서드")
	@Nested
	class ProcessScheduler {

		@Test
		@DisplayName("PG 상태가 SUCCESS 이고 결제가 PENDING 이면 SUCCESS 로 동기화한다")
		void syncsToSuccessWhenPgIsSuccessAndPaymentIsPending() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");

			synchronizer.processScheduler(payment, TransactionStatus.SUCCESS);

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
		}

		@Test
		@DisplayName("PG 상태가 FAILED 이고 결제가 PENDING 이면 FAILED 로 동기화한다")
		void syncsToFailedWhenPgIsFailedAndPaymentIsPending() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");

			synchronizer.processScheduler(payment, TransactionStatus.FAILED);

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.FAILED);
		}

		@Test
		@DisplayName("PG 상태가 PENDING 이면 결제 상태를 변경하지 않는다")
		void doesNotSyncWhenPgIsPending() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");

			synchronizer.processScheduler(payment, TransactionStatus.PENDING);

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.PENDING);
		}

		@Test
		@DisplayName("결제가 이미 SUCCESS 상태이면 상태를 변경하지 않는다")
		void doesNotSyncWhenPaymentAlreadySuccess() {
			Payment payment = paymentWith(TransactionStatus.SUCCESS, "txn-001", "ORD-1001");

			synchronizer.processScheduler(payment, TransactionStatus.FAILED);

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
		}

		@Test
		@DisplayName("결제가 이미 FAILED 상태이면 상태를 변경하지 않는다")
		void doesNotSyncWhenPaymentAlreadyFailed() {
			Payment payment = paymentWith(TransactionStatus.FAILED, "txn-001", "ORD-1001");

			synchronizer.processScheduler(payment, TransactionStatus.SUCCESS);

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.FAILED);
		}
	}

	@DisplayName("processCallback 메서드 - 추가 분기")
	@Nested
	class ProcessCallbackExtended {

		@Test
		@DisplayName("콜백과 PG 데이터의 상태가 불일치하면 PG 실제 상태로 반영한다")
		void usesPgStatusWhenCallbackAndPgStatusMismatch() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");
			PaymentData.ProcessCallback callback = new PaymentData.ProcessCallback(
				"txn-001", TransactionStatus.SUCCESS, "ORD-1001"
			);
			PaymentInfo.Transaction pgTransaction = new PaymentInfo.Transaction(
				"txn-001", "ORD-1001", 10000L, "1234-5678-1234-5678",
				CardType.SAMSUNG, TransactionStatus.FAILED, "승인 거부"
			);

			synchronizer.processCallback(payment, callback, pgTransaction);

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.FAILED);
		}

		@Test
		@DisplayName("PG 트랜잭션의 status 가 null 이면 교차검증 실패로 PG 상태(null)를 사용하여 상태가 변경되지 않는다")
		void doesNotUpdateWhenPgTransactionStatusIsNull() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");
			PaymentData.ProcessCallback callback = new PaymentData.ProcessCallback(
				"txn-001", TransactionStatus.SUCCESS, "ORD-1001"
			);
			PaymentInfo.Transaction pgTransaction = new PaymentInfo.Transaction(
				"txn-001", "ORD-1001", 10000L, "1234-5678-1234-5678",
				CardType.SAMSUNG, null, null
			);

			synchronizer.processCallback(payment, callback, pgTransaction);

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.PENDING);
		}

		@Test
		@DisplayName("PG 트랜잭션 키가 콜백 트랜잭션 키와 다르면 PG 상태로 반영한다")
		void usesPgStatusWhenTransactionKeyMismatch() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");
			PaymentData.ProcessCallback callback = new PaymentData.ProcessCallback(
				"txn-001", TransactionStatus.SUCCESS, "ORD-1001"
			);
			PaymentInfo.Transaction pgTransaction = new PaymentInfo.Transaction(
				"txn-999", "ORD-1001", 10000L, "1234-5678-1234-5678",
				CardType.SAMSUNG, TransactionStatus.SUCCESS, "정상 승인"
			);

			synchronizer.processCallback(payment, callback, pgTransaction);

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
		}

		@Test
		@DisplayName("콜백 주문 ID 가 null 이면 주문 ID 체크를 통과하여 정상 처리된다")
		void passesWhenCallbackOrderIdIsNull() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");
			PaymentData.ProcessCallback callback = new PaymentData.ProcessCallback(
				"txn-001", TransactionStatus.SUCCESS, null
			);
			PaymentInfo.Transaction pgTransaction = new PaymentInfo.Transaction(
				"txn-001", "ORD-1001", 10000L, "1234-5678-1234-5678",
				CardType.SAMSUNG, TransactionStatus.SUCCESS, "정상 승인"
			);

			synchronizer.processCallback(payment, callback, pgTransaction);

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
		}

		@Test
		@DisplayName("중복 상태 콜백이면 상태를 변경하지 않는다")
		void doesNotUpdateWhenDuplicateStatus() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");
			PaymentData.ProcessCallback callback = new PaymentData.ProcessCallback(
				"txn-001", TransactionStatus.PENDING, "ORD-1001"
			);
			PaymentInfo.Transaction pgTransaction = new PaymentInfo.Transaction(
				"txn-001", "ORD-1001", 10000L, "1234-5678-1234-5678",
				CardType.SAMSUNG, TransactionStatus.PENDING, "대기중"
			);

			synchronizer.processCallback(payment, callback, pgTransaction);

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.PENDING);
		}
	}

	@DisplayName("processCallbackWithException 메서드 - 추가 분기")
	@Nested
	class ProcessCallbackWithExceptionExtended {

		@Test
		@DisplayName("주문 ID 가 일치하면 콜백 상태로 업데이트한다")
		void updatesStatusWhenOrderIdMatches() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");
			PaymentData.ProcessCallback callback = new PaymentData.ProcessCallback(
				"txn-001", TransactionStatus.SUCCESS, "ORD-1001"
			);

			synchronizer.processCallbackWithException(payment, callback);

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
		}

		@Test
		@DisplayName("주문 ID 가 불일치하면 상태를 변경하지 않는다")
		void doesNotUpdateWhenOrderIdMismatch() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");
			PaymentData.ProcessCallback callback = new PaymentData.ProcessCallback(
				"txn-001", TransactionStatus.SUCCESS, "ORD-9999"
			);

			synchronizer.processCallbackWithException(payment, callback);

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.PENDING);
		}

		@Test
		@DisplayName("콜백 주문 ID 가 빈 문자열이면 주문 ID 체크를 통과한다")
		void passesWhenCallbackOrderIdIsBlank() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");
			PaymentData.ProcessCallback callback = new PaymentData.ProcessCallback(
				"txn-001", TransactionStatus.FAILED, "   "
			);

			synchronizer.processCallbackWithException(payment, callback);

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.FAILED);
		}
	}

	@DisplayName("내부 분기 보강")
	@Nested
	class InternalBranchCoverage {

		@Test
		@DisplayName("isCallbackDataValid: pgTransaction이 null이면 false")
		void isCallbackDataValid_returnsFalseWhenPgTransactionNull() {
			PaymentData.ProcessCallback callback = new PaymentData.ProcessCallback("txn-001", TransactionStatus.SUCCESS, "ORD-1001");

			Boolean valid = ReflectionTestUtils.invokeMethod(synchronizer, "isCallbackDataValid", callback, null);

			assertThat(valid).isFalse();
		}

		@Test
		@DisplayName("isOrderIdMatched: callbackOrderId가 있고 targetOrderId가 blank면 false")
		void isOrderIdMatched_returnsFalseWhenTargetOrderBlank() {
			Boolean matched = ReflectionTestUtils.invokeMethod(synchronizer, "isOrderIdMatched", "ORD-1001", "   ");

			assertThat(matched).isFalse();
		}

		@Test
		@DisplayName("updatePaymentStatus: 상태가 null이면 변경하지 않는다")
		void updatePaymentStatus_keepsStatusWhenRequestedStatusNull() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");

			ReflectionTestUtils.invokeMethod(synchronizer, "updatePaymentStatus", payment, null);

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.PENDING);
		}

		@Test
		@DisplayName("updatePaymentStatus: 결제가 터미널 상태면 요청 상태를 무시한다")
		void updatePaymentStatus_ignoresWhenPaymentIsNotPending() {
			Payment payment = paymentWith(TransactionStatus.FAILED, "txn-001", "ORD-1001");

			ReflectionTestUtils.invokeMethod(synchronizer, "updatePaymentStatus", payment, TransactionStatus.SUCCESS);

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.FAILED);
		}

		@Test
		@DisplayName("updatePaymentStatus: INITIAL 요청은 PENDING 처리 경로를 따른다")
		void updatePaymentStatus_handlesInitialCaseAsPending() {
			Payment payment = paymentWith(TransactionStatus.PENDING, "txn-001", "ORD-1001");

			ReflectionTestUtils.invokeMethod(synchronizer, "updatePaymentStatus", payment, TransactionStatus.INITIAL);

			assertThat(payment.getStatus()).isEqualTo(TransactionStatus.PENDING);
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
