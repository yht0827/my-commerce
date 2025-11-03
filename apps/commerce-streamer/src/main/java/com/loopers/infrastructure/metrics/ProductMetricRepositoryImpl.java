package com.loopers.infrastructure.metrics;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.loopers.domain.metrics.ProductMetric;
import com.loopers.domain.metrics.ProductMetricRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductMetricRepositoryImpl implements ProductMetricRepository {
	private final ProductMetricJpaRepository productMetricJpaRepository;

	@Override
	public Optional<ProductMetric> findByProductId(Long productId) {
		return productMetricJpaRepository.findByProductId(productId);
	}

	@Override
	public void save(final ProductMetric productMetric) {
		productMetricJpaRepository.save(productMetric);
	}
}
