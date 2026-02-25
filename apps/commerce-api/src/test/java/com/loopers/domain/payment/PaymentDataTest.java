package com.loopers.domain.payment;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PaymentData 테스트")
class PaymentDataTest {

	@Test
	@DisplayName("PaymentRequest.toEntity 는 userId 를 포함해 결제 엔티티를 생성한다")
	void toEntityIncludesUserId() {
		PaymentData.PaymentRequest request = new PaymentData.PaymentRequest(
			"user1234",
			"ORD-1001",
			CardType.KB,
			"1234-5678-1234-5678",
			10000L,
			"https://callback.example.com/payments"
		);

		Payment payment = request.toEntity();

		assertThat(payment.getUserId()).isNotNull();
		assertThat(payment.getUserId().getUserId()).isEqualTo("user1234");
		assertThat(payment.getStatus()).isEqualTo(TransactionStatus.PENDING);
	}
}
