package com.loopers.domain.coupon;

import java.util.Optional;

public interface CouponRepository {

	Optional<Coupon> findById(final Long id);

	Optional<Coupon> findByIdWithPessimisticLock(final Long id);

	Optional<Coupon> findByIdWithOptimisticLock(final Long id);

	Coupon save(final Coupon coupon);
}
