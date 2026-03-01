package com.loopers.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductCounterEventHistoryRepository {

	boolean createIfNotExists(String dedupeKey, ProductId productId, ProductCounterType counterType);

	Optional<ProductCounterEventHistory> findByDedupeKey(String dedupeKey);

	long countCompletedByProductIdAndCounterType(ProductId productId, ProductCounterType counterType);

	List<ProductCounterEventHistory> findFailedEvents(int limit);
}
