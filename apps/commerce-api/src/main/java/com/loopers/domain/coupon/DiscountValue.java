package com.loopers.domain.coupon;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

import com.loopers.support.error.CoreException;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiscountValue implements Serializable {

	@Column(name = "discount_value")
	private Long discountValue;

	public DiscountValue(Long discountValue) {
		if (discountValue == null || discountValue < 0) {
			throw new CoreException(BAD_REQUEST, COUPON_DISCOUNT_VALUE_INVALID.format());
		}
		this.discountValue = discountValue;
	}

	public Long calculateFixedDiscount(Long amount) {
		return Math.min(discountValue, amount);
	}

	public Long calculatePercentageDiscount(Long amount) {
		BigDecimal discountAmount = BigDecimal.valueOf(amount)
			.multiply(BigDecimal.valueOf(discountValue))
			.divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
		return discountAmount.longValue();
	}

}
