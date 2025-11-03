package com.loopers.infrastructure.metrics;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.metrics.ProductRankDaily;

public interface ProductRankDailyJpaRepository extends JpaRepository<ProductRankDaily, Long> {
}
