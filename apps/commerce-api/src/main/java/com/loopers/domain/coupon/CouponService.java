package com.loopers.domain.coupon;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import org.springframework.stereotype.Service;

import com.loopers.domain.order.CouponDiscountAmount;
import com.loopers.domain.order.TotalOrderPrice;
import com.loopers.support.error.CoreException;

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
			.orElseThrow(() -> new CoreException(NOT_FOUND, COUPON_NOT_FOUND.format(couponId)));

		CouponDiscountAmount couponDiscountAmount = coupon.applyDiscount(totalOrderPrice);
		couponRepository.save(coupon);

		return couponDiscountAmount;
	}
}
