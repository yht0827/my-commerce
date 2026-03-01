package com.loopers.domain.coupon;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("DiscountPolicy 테스트")
class DiscountPolicyTest {

	@DisplayName("정률 쿠폰 생성 시")
	@Nested
	class PercentageCoupon {

		@Test
		@DisplayName("할인율이 100을 초과하면 실패한다")
		void failWhenPercentageValueIsGreaterThan100() {
			CoreException result = assertThrows(CoreException.class,
				() -> new DiscountPolicy(new DiscountValue(101L), null, CouponType.PERCENTAGE));

			assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
		}

		@Test
		@DisplayName("할인율이 100이면 주문 금액 전체를 할인한다")
		void discountAllWhenPercentageValueIs100() {
			DiscountPolicy discountPolicy = new DiscountPolicy(new DiscountValue(100L), null, CouponType.PERCENTAGE);

			Long discountAmount = discountPolicy.calculate(27500L);

			assertThat(discountAmount).isEqualTo(27500L);
		}
	}

	@Test
	@DisplayName("정액 쿠폰은 100 초과 값도 정상 처리한다")
	void fixedAmountCouponAllowsValueGreaterThan100() {
		DiscountPolicy discountPolicy = new DiscountPolicy(new DiscountValue(10000L), null, CouponType.FIXED_AMOUNT);

		Long discountAmount = discountPolicy.calculate(3000L);

		assertThat(discountAmount).isEqualTo(3000L);
	}
}
