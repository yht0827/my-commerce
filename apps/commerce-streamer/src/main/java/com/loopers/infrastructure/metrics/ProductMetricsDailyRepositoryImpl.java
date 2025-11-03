package com.loopers.infrastructure.metrics;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.loopers.domain.metrics.ProductMetricsDaily;
import com.loopers.domain.metrics.ProductMetricsDailyRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductMetricsDailyRepositoryImpl implements ProductMetricsDailyRepository {

	private final ProductMetricsDailyJpaRepository productMetricsDailyJpaRepository;

	@Override
	public List<ProductMetricsDaily> findAllBySnapshotDateAndProductIdIn(final LocalDate date, final Collection<Long> ids) {
		return productMetricsDailyJpaRepository.findAllBySnapshotDateAndProductIdIn(date, ids);
	}

	@Override
	public void saveAll(Iterable<ProductMetricsDaily> entities) {
		productMetricsDailyJpaRepository.saveAll(entities);
	}
}

