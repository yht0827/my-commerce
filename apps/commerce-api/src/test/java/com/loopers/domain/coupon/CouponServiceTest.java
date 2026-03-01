package com.loopers.domain.coupon;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.loopers.domain.order.CouponDiscountAmount;
import com.loopers.domain.order.TotalOrderPrice;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("쿠폰 서비스 단위 테스트")
class CouponServiceTest {

	@Test
	@DisplayName("couponId가 없으면 할인 금액 0을 반환한다")
	void returnsZeroDiscount_whenCouponIdIsNull() {
		// arrange
		CouponRepository couponRepository = mock(CouponRepository.class);
		CouponService couponService = new CouponService(couponRepository);

		// act
		CouponDiscountAmount result = couponService.applyDiscount(null, new TotalOrderPrice(1000L));

		// assert
		assertThat(result.getCouponDiscountAmount()).isZero();
		verifyNoInteractions(couponRepository);
	}

	@Test
	@DisplayName("쿠폰이 없으면 NOT_FOUND 예외가 발생한다")
	void throwsNotFound_whenCouponDoesNotExist() {
		// arrange
		CouponRepository couponRepository = mock(CouponRepository.class);
		CouponService couponService = new CouponService(couponRepository);
		Long couponId = 1L;
		when(couponRepository.findByIdWithPessimisticLock(couponId)).thenReturn(Optional.empty());

		// act
		CoreException result = assertThrows(
			CoreException.class,
			() -> couponService.applyDiscount(couponId, new TotalOrderPrice(1000L))
		);

		// assert
		assertThat(result.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
	}

	@Test
	@DisplayName("쿠폰이 있으면 할인 적용 후 쿠폰 상태를 저장한다")
	void appliesDiscountAndSavesCoupon_whenCouponExists() {
		// arrange
		CouponRepository couponRepository = mock(CouponRepository.class);
		CouponService couponService = new CouponService(couponRepository);
		Long couponId = 1L;
		Coupon coupon = mock(Coupon.class);
		TotalOrderPrice totalOrderPrice = new TotalOrderPrice(1000L);
		CouponDiscountAmount expected = CouponDiscountAmount.of(100L);

		when(couponRepository.findByIdWithPessimisticLock(couponId)).thenReturn(Optional.of(coupon));
		when(coupon.applyDiscount(totalOrderPrice)).thenReturn(expected);
		when(couponRepository.save(coupon)).thenReturn(coupon);

		// act
		CouponDiscountAmount result = couponService.applyDiscount(couponId, totalOrderPrice);

		// assert
		assertThat(result).isEqualTo(expected);
		verify(couponRepository).save(coupon);
	}
}
