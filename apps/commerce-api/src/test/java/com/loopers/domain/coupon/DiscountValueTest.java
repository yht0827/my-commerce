package com.loopers.domain.coupon;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DiscountValue 테스트")
class DiscountValueTest {

	@Test
	@DisplayName("정률 할인은 소수점 이하는 버림 처리된다")
	void percentageDiscountRoundsDownFractionalPart() {
		DiscountValue discountValue = new DiscountValue(33L);

		Long discountAmount = discountValue.calculatePercentageDiscount(1003L);

		assertThat(discountAmount).isEqualTo(330L);
	}

	@Test
	@DisplayName("정액 할인은 대상 금액을 초과할 수 없다")
	void fixedDiscountCannotExceedTargetAmount() {
		DiscountValue discountValue = new DiscountValue(10000L);

		Long discountAmount = discountValue.calculateFixedDiscount(7000L);

		assertThat(discountAmount).isEqualTo(7000L);
	}
}
