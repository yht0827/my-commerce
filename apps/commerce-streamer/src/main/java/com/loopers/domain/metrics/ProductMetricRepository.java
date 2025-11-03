package com.loopers.domain.metrics;

import java.util.Optional;

public interface ProductMetricRepository {

	Optional<ProductMetric> findByProductId(Long productId);

	void save(ProductMetric productMetric);
}
