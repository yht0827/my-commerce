package com.loopers.domain.coupon;

import java.io.Serializable;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

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

	@Embedded
	private DiscountValue discountValue;

	@Embedded
	private MaxDiscountAmount maxDiscountAmount;

	@Enumerated(EnumType.STRING)
	private CouponType couponType;

	public DiscountPolicy(final DiscountValue discountValue, final MaxDiscountAmount maxDiscountAmount,
		final CouponType couponType) {
		if (discountValue == null) {
			throw new CoreException(ErrorType.BAD_REQUEST, "할인 값이 설정되지 않았습니다.");
		}
		if (couponType == null) {
			throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 타입이 설정되지 않았습니다.");
		}
		this.discountValue = discountValue;
		this.maxDiscountAmount = maxDiscountAmount;
		this.couponType = couponType;
	}

	public Long calculate(final Long amount) {
		if (amount == null || amount < 0) {
			throw new CoreException(ErrorType.BAD_REQUEST, "할인 계산 대상 금액은 0 이상이어야 합니다.");
		}

		Long discount = (couponType == CouponType.FIXED_AMOUNT)
			? discountValue.calculateFixedDiscount(amount)
			: discountValue.calculatePercentageDiscount(amount);

		return (maxDiscountAmount != null && maxDiscountAmount.hasLimit())
			? maxDiscountAmount.applyMaxDiscountLimit(discount)
			: discount;
	}
}
