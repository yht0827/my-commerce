package com.loopers.infrastructure.metrics;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.metrics.ProductMetric;

public interface ProductMetricJpaRepository extends JpaRepository<ProductMetric, Long> {

	Optional<ProductMetric> findByProductId(Long productId);
}
