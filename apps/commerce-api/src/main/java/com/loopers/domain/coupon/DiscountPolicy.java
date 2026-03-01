package com.loopers.domain.coupon;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import java.io.Serializable;

import com.loopers.support.error.CoreException;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiscountPolicy implements Serializable {

	private static final long MAX_PERCENTAGE_DISCOUNT_VALUE = 100L;

	@Embedded
	private DiscountValue discountValue;

	@Embedded
	private MaxDiscountAmount maxDiscountAmount;

	@Enumerated(EnumType.STRING)
	private CouponType couponType;

	public DiscountPolicy(final DiscountValue discountValue, final MaxDiscountAmount maxDiscountAmount,
		final CouponType couponType) {
		if (discountValue == null) {
			throw new CoreException(BAD_REQUEST, COUPON_DISCOUNT_VALUE_REQUIRED.format());
		}
		if (couponType == null) {
			throw new CoreException(BAD_REQUEST, COUPON_TYPE_REQUIRED.format());
		}
		if (couponType == CouponType.PERCENTAGE && discountValue.getDiscountValue() > MAX_PERCENTAGE_DISCOUNT_VALUE) {
			throw new CoreException(BAD_REQUEST, COUPON_PERCENTAGE_DISCOUNT_VALUE_INVALID.format());
		}
		this.discountValue = discountValue;
		this.maxDiscountAmount = maxDiscountAmount;
		this.couponType = couponType;
	}

	public Long calculate(final Long amount) {
		if (amount == null || amount < 0) {
			throw new CoreException(BAD_REQUEST, COUPON_DISCOUNT_TARGET_AMOUNT_INVALID.format());
		}

		Long discount = (couponType == CouponType.FIXED_AMOUNT)
			? discountValue.calculateFixedDiscount(amount)
			: discountValue.calculatePercentageDiscount(amount);

		return (maxDiscountAmount != null && maxDiscountAmount.hasLimit())
			? maxDiscountAmount.applyMaxDiscountLimit(discount)
			: discount;
	}
}
