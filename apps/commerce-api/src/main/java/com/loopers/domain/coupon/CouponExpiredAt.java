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
public class CouponExpiredAt implements Serializable {

	@Column(name = "expired_at")
	private ZonedDateTime couponExpiredAt;
	
	public CouponExpiredAt(ZonedDateTime couponExpiredAt) {
		if (couponExpiredAt == null) {
			throw new CoreException(BAD_REQUEST, COUPON_EXPIRED_AT_REQUIRED.format());
		}

		if (couponExpiredAt.isBefore(ZonedDateTime.now())) {
			throw new CoreException(BAD_REQUEST, COUPON_EXPIRED_AT_INVALID.format());
		}
		this.couponExpiredAt = couponExpiredAt;
	}
}
