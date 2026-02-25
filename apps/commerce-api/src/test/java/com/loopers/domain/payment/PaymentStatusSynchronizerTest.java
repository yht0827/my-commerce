package com.loopers.domain.payment;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.loopers.domain.common.Price;
import com.loopers.domain.order.OrderId;
import com.loopers.domain.user.UserId;

@DisplayName("PaymentStatusSynchronizer 테스트")
class PaymentStatusSynchronizerTest {

	private final PaymentStatusSynchronizer synchronizer = new PaymentStatusSynchronizer();

	@Test
	@DisplayName("콜백 데이터와 PG 조회 결과가 일치하면 결제 상태를 SUCCESS 로 반영한다")
	void processCallbackWithValidDataUpdatesStatus() {
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

		synchronizer.processCallback(payment, callback, pgTransaction);

		assertThat(payment.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
	}

	@Test
	@DisplayName("콜백 주문 ID 가 결제 주문 ID 와 다르면 상태를 변경하지 않는다")
	void processCallbackWithOrderMismatchDoesNotUpdate() {
		Payment payment = paymentWith(TransactionStatus.PENDING, "20260101:TR:abc123", "ORD-1001");
		PaymentData.ProcessCallback callback = new PaymentData.ProcessCallback(
			"20260101:TR:abc123",
			TransactionStatus.SUCCESS,
			"ORD-9999"
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

		synchronizer.processCallback(payment, callback, pgTransaction);

		assertThat(payment.getStatus()).isEqualTo(TransactionStatus.PENDING);
	}

	@Test
	@DisplayName("터미널 상태(SUCCESS) 결제에 대한 다른 상태 요청은 무시한다")
	void terminalStatusIgnoresFurtherTransition() {
		Payment payment = paymentWith(TransactionStatus.SUCCESS, "20260101:TR:abc123", "ORD-1001");
		PaymentData.ProcessCallback callback = new PaymentData.ProcessCallback(
			"20260101:TR:abc123",
			TransactionStatus.FAILED,
			"ORD-1001"
		);

		synchronizer.processCallbackWithException(payment, callback);

		assertThat(payment.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
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
