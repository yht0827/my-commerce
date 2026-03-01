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
public class CouponIssuedAt implements Serializable {

	@Column(name = "issued_at")
	private ZonedDateTime couponIssuedAt;
	
	public CouponIssuedAt(ZonedDateTime couponIssuedAt) {
		if (couponIssuedAt == null) {
			throw new CoreException(BAD_REQUEST, COUPON_ISSUED_AT_REQUIRED.format());
		}

		if (couponIssuedAt.isAfter(ZonedDateTime.now())) {
			throw new CoreException(BAD_REQUEST, COUPON_ISSUED_AT_INVALID.format());
		}
		this.couponIssuedAt = couponIssuedAt;
	}
}
