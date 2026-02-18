package com.loopers.domain.coupon;

import org.springframework.stereotype.Service;

import com.loopers.domain.order.CouponDiscountAmount;
import com.loopers.domain.order.TotalOrderPrice;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponService {

	private final CouponRepository couponRepository;

	public CouponDiscountAmount applyDiscount(final Long couponId, final TotalOrderPrice totalOrderPrice) {
		if (couponId == null) {
			return CouponDiscountAmount.of(0L);
		}

		Coupon coupon = couponRepository.findByIdWithPessimisticLock(couponId)
			.orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "해당 [id = " + couponId + "]의 쿠폰이 존재하지 않습니다."));

		CouponDiscountAmount couponDiscountAmount = coupon.applyDiscount(totalOrderPrice);
		couponRepository.save(coupon);

		return couponDiscountAmount;
	}
}
