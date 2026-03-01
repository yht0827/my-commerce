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

	@DisplayName("생성 시 유효성 검증")
	@Nested
	class Validation {

		@Test
		@DisplayName("discountValue 가 null 이면 BAD_REQUEST 예외가 발생한다")
		void failsWhenDiscountValueIsNull() {
			assertThatThrownBy(() -> new DiscountPolicy(null, null, CouponType.FIXED_AMOUNT))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
		}

		@Test
		@DisplayName("couponType 이 null 이면 BAD_REQUEST 예외가 발생한다")
		void failsWhenCouponTypeIsNull() {
			assertThatThrownBy(() -> new DiscountPolicy(new DiscountValue(1000L), null, null))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
		}
	}

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

		@Test
		@DisplayName("정률 할인 시 비율 계산이 정확하다")
		void calculatesPercentageCorrectly() {
			DiscountPolicy discountPolicy = new DiscountPolicy(new DiscountValue(15L), null, CouponType.PERCENTAGE);

			Long discountAmount = discountPolicy.calculate(20000L);

			assertThat(discountAmount).isEqualTo(3000L);
		}

		@Test
		@DisplayName("maxDiscountAmount 가 설정되면 할인 한도가 적용된다")
		void appliesMaxDiscountAmountLimit() {
			DiscountPolicy discountPolicy = new DiscountPolicy(
				new DiscountValue(50L), new MaxDiscountAmount(3000L), CouponType.PERCENTAGE
			);

			Long discountAmount = discountPolicy.calculate(20000L);

			assertThat(discountAmount).isEqualTo(3000L);
		}

		@Test
		@DisplayName("maxDiscountAmount 가 null 이면 할인 한도 없이 전액 할인한다")
		void noLimitWhenMaxDiscountAmountIsNull() {
			DiscountPolicy discountPolicy = new DiscountPolicy(
				new DiscountValue(50L), null, CouponType.PERCENTAGE
			);

			Long discountAmount = discountPolicy.calculate(20000L);

			assertThat(discountAmount).isEqualTo(10000L);
		}

		@Test
		@DisplayName("maxDiscountAmount 가 계산된 할인보다 크면 계산 할인 그대로 적용된다")
		void usesCalculatedDiscountWhenBelowMaxLimit() {
			DiscountPolicy discountPolicy = new DiscountPolicy(
				new DiscountValue(10L), new MaxDiscountAmount(50000L), CouponType.PERCENTAGE
			);

			Long discountAmount = discountPolicy.calculate(20000L);

			assertThat(discountAmount).isEqualTo(2000L);
		}
	}

	@DisplayName("정액 쿠폰")
	@Nested
	class FixedAmountCoupon {

		@Test
		@DisplayName("할인액이 주문금액보다 작으면 할인액을 그대로 반환한다")
		void returnsDiscountValueWhenLessThanAmount() {
			DiscountPolicy discountPolicy = new DiscountPolicy(new DiscountValue(3000L), null, CouponType.FIXED_AMOUNT);

			Long discountAmount = discountPolicy.calculate(10000L);

			assertThat(discountAmount).isEqualTo(3000L);
		}

		@Test
		@DisplayName("할인액이 주문금액보다 크면 주문금액을 반환한다")
		void returnsAmountWhenDiscountExceedsAmount() {
			DiscountPolicy discountPolicy = new DiscountPolicy(new DiscountValue(10000L), null, CouponType.FIXED_AMOUNT);

			Long discountAmount = discountPolicy.calculate(3000L);

			assertThat(discountAmount).isEqualTo(3000L);
		}

		@Test
		@DisplayName("100 초과 값도 정상 처리한다")
		void fixedAmountCouponAllowsValueGreaterThan100() {
			DiscountPolicy discountPolicy = new DiscountPolicy(new DiscountValue(10000L), null, CouponType.FIXED_AMOUNT);

			Long discountAmount = discountPolicy.calculate(3000L);

			assertThat(discountAmount).isEqualTo(3000L);
		}

		@Test
		@DisplayName("maxDiscountAmount 가 설정되어도 정액 할인에 한도가 적용된다")
		void appliesMaxDiscountAmountToFixedAmount() {
			DiscountPolicy discountPolicy = new DiscountPolicy(
				new DiscountValue(5000L), new MaxDiscountAmount(3000L), CouponType.FIXED_AMOUNT
			);

			Long discountAmount = discountPolicy.calculate(10000L);

			assertThat(discountAmount).isEqualTo(3000L);
		}
	}

	@DisplayName("calculate 입력값 검증")
	@Nested
	class CalculateValidation {

		@Test
		@DisplayName("amount 가 null 이면 BAD_REQUEST 예외가 발생한다")
		void throwsWhenAmountIsNull() {
			DiscountPolicy discountPolicy = new DiscountPolicy(new DiscountValue(1000L), null, CouponType.FIXED_AMOUNT);

			assertThatThrownBy(() -> discountPolicy.calculate(null))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
		}

		@Test
		@DisplayName("amount 가 음수면 BAD_REQUEST 예외가 발생한다")
		void throwsWhenAmountIsNegative() {
			DiscountPolicy discountPolicy = new DiscountPolicy(new DiscountValue(1000L), null, CouponType.FIXED_AMOUNT);

			assertThatThrownBy(() -> discountPolicy.calculate(-1L))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
		}

		@Test
		@DisplayName("amount 가 0 이면 정상적으로 0 을 반환한다")
		void returnsZeroWhenAmountIsZero() {
			DiscountPolicy discountPolicy = new DiscountPolicy(new DiscountValue(1000L), null, CouponType.FIXED_AMOUNT);

			Long discountAmount = discountPolicy.calculate(0L);

			assertThat(discountAmount).isEqualTo(0L);
		}
	}
}
