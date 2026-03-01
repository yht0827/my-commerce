package com.loopers.domain.payment;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.loopers.support.error.CoreException;

@DisplayName("Payment 값 객체 테스트")
class PaymentValueObjectTest {

	@Test
	@DisplayName("CardNo: null 또는 형식 불일치면 예외가 발생한다")
	void cardNo_throwsWhenNullOrInvalidFormat() {
		assertThatThrownBy(() -> new CardNo(null)).isInstanceOf(CoreException.class);
		assertThatThrownBy(() -> new CardNo("1111222233334444")).isInstanceOf(CoreException.class);
	}

	@Test
	@DisplayName("CardNo: 올바른 형식이면 정상 생성된다")
	void cardNo_createsWhenValidFormat() {
		CardNo cardNo = new CardNo("1111-2222-3333-4444");

		assertThat(ReflectionTestUtils.getField(cardNo, "cardNo")).isEqualTo("1111-2222-3333-4444");
	}

	@Test
	@DisplayName("PaymentReason: null 또는 blank면 예외가 발생한다")
	void paymentReason_throwsWhenNullOrBlank() {
		assertThatThrownBy(() -> new PaymentReason(null)).isInstanceOf(CoreException.class);
		assertThatThrownBy(() -> new PaymentReason("   ")).isInstanceOf(CoreException.class);
	}

	@Test
	@DisplayName("PaymentReason: 정상 문자열이면 생성된다")
	void paymentReason_createsWhenValid() {
		PaymentReason reason = new PaymentReason("결제 요청");

		assertThat(reason.reason()).isEqualTo("결제 요청");
	}

	@Test
	@DisplayName("CallbackUrl: null 또는 https가 아니면 예외가 발생한다")
	void callbackUrl_throwsWhenNullOrNotHttps() {
		assertThatThrownBy(() -> new CallbackUrl(null)).isInstanceOf(CoreException.class);
		assertThatThrownBy(() -> new CallbackUrl("http://callback.example.com")).isInstanceOf(CoreException.class);
	}

	@Test
	@DisplayName("CallbackUrl: https로 시작하면 생성된다")
	void callbackUrl_createsWhenHttps() {
		CallbackUrl callbackUrl = new CallbackUrl("https://callback.example.com/payments");

		assertThat(ReflectionTestUtils.getField(callbackUrl, "callbackUrl"))
			.isEqualTo("https://callback.example.com/payments");
	}

	@Test
	@DisplayName("TransactionKey: null 또는 blank면 예외가 발생한다")
	void transactionKey_throwsWhenNullOrBlank() {
		assertThatThrownBy(() -> new TransactionKey(null)).isInstanceOf(CoreException.class);
		assertThatThrownBy(() -> new TransactionKey("   ")).isInstanceOf(CoreException.class);
	}

	@Test
	@DisplayName("TransactionKey: 정상 문자열이면 생성된다")
	void transactionKey_createsWhenValid() {
		TransactionKey transactionKey = new TransactionKey("TR-20260301-0001");

		assertThat(ReflectionTestUtils.getField(transactionKey, "transactionKey")).isEqualTo("TR-20260301-0001");
	}
}
