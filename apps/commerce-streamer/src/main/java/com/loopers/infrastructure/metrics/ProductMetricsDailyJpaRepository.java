package com.loopers.infrastructure.metrics;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.metrics.ProductMetricsDaily;

public interface ProductMetricsDailyJpaRepository extends JpaRepository<ProductMetricsDaily, Long> {

	List<ProductMetricsDaily> findAllBySnapshotDateAndProductIdIn(LocalDate date, Collection<Long> ids);
}

