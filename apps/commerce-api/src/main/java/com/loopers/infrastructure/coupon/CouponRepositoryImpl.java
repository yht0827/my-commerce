package com.loopers.infrastructure.coupon;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class CouponRepositoryImpl implements CouponRepository {

	private final CouponJpaRepository couponJpaRepository;

	@Override
	public Optional<Coupon> findById(final Long id) {
		return couponJpaRepository.findById(id);
	}

	@Override
	public Optional<Coupon> findByIdWithPessimisticLock(final Long id) {
		return couponJpaRepository.findByIdWithPessimisticLock(id);
	}

	@Override
	public Optional<Coupon> findByIdWithOptimisticLock(final Long id) {
		return couponJpaRepository.findByIdWithOptimisticLock(id);
	}

	@Override
	public Coupon save(final Coupon coupon) {
		return couponJpaRepository.save(coupon);
	}
}
