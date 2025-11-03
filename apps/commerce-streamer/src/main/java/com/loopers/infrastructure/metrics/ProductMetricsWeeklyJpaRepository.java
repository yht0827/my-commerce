package com.loopers.infrastructure.metrics;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.metrics.ProductMetricsWeekly;

public interface ProductMetricsWeeklyJpaRepository extends JpaRepository<ProductMetricsWeekly, Long> {
}

