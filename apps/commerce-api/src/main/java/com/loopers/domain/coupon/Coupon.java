package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.BrandId;
import com.loopers.domain.order.CouponDiscountAmount;
import com.loopers.domain.order.TotalOrderPrice;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.user.UserId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "coupons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

	@Embedded
	private UserId userId;

	@Embedded
	private ProductId productId;

	@Embedded
	private BrandId brandId;

	@Embedded
	private CouponName couponName;

	@Embedded
	private DiscountPolicy discountPolicy;

	@Embedded
	private CouponIssuedAt couponIssuedAt;

	@Embedded
	private CouponUsedAt couponUsedAt;

	@Embedded
	private CouponExpiredAt couponExpiredAt;

	@Enumerated(EnumType.STRING)
	private CouponStatus couponStatus;

	@Version
	private Long version;

	@Builder
	public Coupon(UserId userId, ProductId productId, BrandId brandId, CouponName couponName, DiscountValue discountValue,
		MaxDiscountAmount maxDiscountAmount, CouponType couponType, CouponIssuedAt couponIssuedAt, CouponUsedAt couponUsedAt,
		CouponExpiredAt couponExpiredAt, CouponStatus couponStatus) {
		this.userId = userId;
		this.productId = productId;
		this.brandId = brandId;
		this.couponName = couponName;
		this.discountPolicy = new DiscountPolicy(discountValue, maxDiscountAmount, couponType);
		this.couponIssuedAt = couponIssuedAt;
		this.couponUsedAt = couponUsedAt;
		this.couponExpiredAt = couponExpiredAt;
		this.couponStatus = couponStatus;
	}

	public void markAsUsed() {
		this.couponUsedAt.update();
		this.couponStatus = CouponStatus.USED;
	}

	public Long calculateDiscount(final Long amount) {
		return discountPolicy.calculate(amount);
	}

	public CouponDiscountAmount applyDiscount(TotalOrderPrice totalOrderPrice) {
		if (couponStatus == CouponStatus.USED) {
			throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용된 쿠폰입니다.");
		}
		Long discountAmount = calculateDiscount(totalOrderPrice.getTotalPrice());
		markAsUsed();
		return CouponDiscountAmount.of(discountAmount);
	}

}
