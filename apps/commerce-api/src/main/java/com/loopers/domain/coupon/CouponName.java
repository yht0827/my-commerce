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
public class CouponName implements Serializable {
	
	@Column(name = "coupon_name")
	private String couponName;
	
	public CouponName(String couponName) {
		if (couponName == null || couponName.isBlank()) {
			throw new CoreException(BAD_REQUEST, COUPON_NAME_REQUIRED.format());
		}
		this.couponName = couponName;
	}
}
