package com.loopers.domain.coupon;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import java.io.Serializable;

import com.loopers.support.error.CoreException;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MaxDiscountAmount implements Serializable {

	@Column(name = "max_discount_amount")
	private Long maxDiscountAmount;

	public MaxDiscountAmount(final Long maxDiscountAmount) {
		if (maxDiscountAmount == null || maxDiscountAmount < 0) {
			throw new CoreException(BAD_REQUEST, COUPON_MAX_DISCOUNT_AMOUNT_INVALID.format());
		}
		this.maxDiscountAmount = maxDiscountAmount;
	}

	public Long applyMaxDiscountLimit(final Long calculatedDiscount) {
		return Math.min(calculatedDiscount, maxDiscountAmount);
	}

	public boolean hasLimit() {
		return maxDiscountAmount != null;
	}
}
