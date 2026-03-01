package com.loopers.domain.coupon;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import java.io.Serializable;
import java.time.ZonedDateTime;

import com.loopers.support.error.CoreException;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponUsedAt implements Serializable {

	@Column(name = "used_at")
	private ZonedDateTime couponUsedAt;
	
	public CouponUsedAt(ZonedDateTime couponUsedAt) {
		if (couponUsedAt == null) {
			throw new CoreException(BAD_REQUEST, COUPON_USED_AT_REQUIRED.format());
		}
		this.couponUsedAt = couponUsedAt;
	}

	public void update() {
		this.couponUsedAt = ZonedDateTime.now();
	}
}
