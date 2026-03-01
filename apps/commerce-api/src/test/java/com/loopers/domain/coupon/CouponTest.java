package com.loopers.domain.coupon;

import static org.assertj.core.api.Assertions.*;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.loopers.domain.brand.BrandId;
import com.loopers.domain.order.CouponDiscountAmount;
import com.loopers.domain.order.TotalOrderPrice;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.user.UserId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("Coupon 도메인 테스트")
class CouponTest {

	@DisplayName("applyDiscount 메서드")
	@Nested
	class ApplyDiscount {

		@Test
		@DisplayName("USED 상태 쿠폰에 할인을 적용하면 BAD_REQUEST 예외가 발생한다")
		void throwsWhenCouponAlreadyUsed() {
			Coupon coupon = couponWith(CouponStatus.USED, CouponType.FIXED_AMOUNT, 3000L, null);
			TotalOrderPrice totalOrderPrice = new TotalOrderPrice(10000L);

			assertThatThrownBy(() -> coupon.applyDiscount(totalOrderPrice))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
		}

		@Test
		@DisplayName("ACTIVE 상태 쿠폰에 정액 할인을 적용하면 할인 금액을 반환하고 USED 로 변경된다")
		void appliesFixedAmountDiscountAndChangesToUsed() {
			Coupon coupon = couponWith(CouponStatus.ACTIVE, CouponType.FIXED_AMOUNT, 3000L, null);
			TotalOrderPrice totalOrderPrice = new TotalOrderPrice(10000L);

			CouponDiscountAmount result = coupon.applyDiscount(totalOrderPrice);

			assertThat(result.getCouponDiscountAmount()).isEqualTo(3000L);
			assertThat(coupon.getCouponStatus()).isEqualTo(CouponStatus.USED);
		}

		@Test
		@DisplayName("ACTIVE 상태 쿠폰에 정률 할인을 적용하면 비율에 맞는 할인 금액을 반환한다")
		void appliesPercentageDiscountCorrectly() {
			Coupon coupon = couponWith(CouponStatus.ACTIVE, CouponType.PERCENTAGE, 10L, null);
			TotalOrderPrice totalOrderPrice = new TotalOrderPrice(25000L);

			CouponDiscountAmount result = coupon.applyDiscount(totalOrderPrice);

			assertThat(result.getCouponDiscountAmount()).isEqualTo(2500L);
			assertThat(coupon.getCouponStatus()).isEqualTo(CouponStatus.USED);
		}

		@Test
		@DisplayName("정률 할인에 최대 할인 금액이 설정되면 제한이 적용된다")
		void appliesPercentageDiscountWithMaxLimit() {
			Coupon coupon = couponWith(CouponStatus.ACTIVE, CouponType.PERCENTAGE, 50L, 5000L);
			TotalOrderPrice totalOrderPrice = new TotalOrderPrice(20000L);

			CouponDiscountAmount result = coupon.applyDiscount(totalOrderPrice);

			assertThat(result.getCouponDiscountAmount()).isEqualTo(5000L);
		}
	}

	@DisplayName("restore 메서드")
	@Nested
	class Restore {

		@Test
		@DisplayName("USED 상태 쿠폰을 ACTIVE 로 복원한다")
		void restoresUsedCouponToActive() {
			Coupon coupon = couponWith(CouponStatus.USED, CouponType.FIXED_AMOUNT, 3000L, null);

			coupon.restore();

			assertThat(coupon.getCouponStatus()).isEqualTo(CouponStatus.ACTIVE);
		}

		@Test
		@DisplayName("EXPIRED 상태 쿠폰도 ACTIVE 로 복원한다")
		void restoresExpiredCouponToActive() {
			Coupon coupon = couponWith(CouponStatus.EXPIRED, CouponType.FIXED_AMOUNT, 3000L, null);

			coupon.restore();

			assertThat(coupon.getCouponStatus()).isEqualTo(CouponStatus.ACTIVE);
		}
	}

	@DisplayName("markAsUsed 메서드")
	@Nested
	class MarkAsUsed {

		@Test
		@DisplayName("쿠폰 상태를 USED 로 변경하고 사용 시각을 갱신한다")
		void changesStatusToUsed() {
			Coupon coupon = couponWith(CouponStatus.ACTIVE, CouponType.FIXED_AMOUNT, 3000L, null);

			coupon.markAsUsed();

			assertThat(coupon.getCouponStatus()).isEqualTo(CouponStatus.USED);
			assertThat(coupon.getCouponUsedAt().getCouponUsedAt()).isNotNull();
		}
	}

	@DisplayName("calculateDiscount 메서드")
	@Nested
	class CalculateDiscount {

		@Test
		@DisplayName("정액 쿠폰은 할인액과 주문금액 중 작은 값을 반환한다")
		void fixedAmountReturnsMinOfDiscountAndAmount() {
			Coupon coupon = couponWith(CouponStatus.ACTIVE, CouponType.FIXED_AMOUNT, 5000L, null);

			Long discount = coupon.calculateDiscount(3000L);

			assertThat(discount).isEqualTo(3000L);
		}

		@Test
		@DisplayName("정률 쿠폰은 비율을 곱한 할인 금액을 반환한다")
		void percentageReturnsCalculatedDiscount() {
			Coupon coupon = couponWith(CouponStatus.ACTIVE, CouponType.PERCENTAGE, 20L, null);

			Long discount = coupon.calculateDiscount(10000L);

			assertThat(discount).isEqualTo(2000L);
		}
	}

	private Coupon couponWith(CouponStatus status, CouponType couponType, Long discountValue, Long maxDiscountAmount) {
		return Coupon.builder()
			.userId(new UserId("user1234"))
			.productId(new ProductId(1L))
			.brandId(new BrandId(1L))
			.couponName(new CouponName("테스트 쿠폰"))
			.discountValue(new DiscountValue(discountValue))
			.maxDiscountAmount(maxDiscountAmount != null ? new MaxDiscountAmount(maxDiscountAmount) : null)
			.couponType(couponType)
			.couponIssuedAt(new CouponIssuedAt(ZonedDateTime.now().minusDays(1)))
			.couponUsedAt(new CouponUsedAt(ZonedDateTime.now().minusHours(1)))
			.couponExpiredAt(new CouponExpiredAt(ZonedDateTime.now().plusDays(30)))
			.couponStatus(status)
			.build();
	}
}
