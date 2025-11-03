package com.loopers.infrastructure.metrics;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.metrics.ProductMetricsMonthly;

public interface ProductMetricsMonthlyJpaRepository extends JpaRepository<ProductMetricsMonthly, Long> {
}

