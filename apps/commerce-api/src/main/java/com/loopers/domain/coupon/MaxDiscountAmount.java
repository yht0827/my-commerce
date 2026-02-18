package com.loopers.domain.coupon;

import java.io.Serializable;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

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
			throw new CoreException(ErrorType.BAD_REQUEST, "최대 할인 금액은 0 이상이어야 합니다.");
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
