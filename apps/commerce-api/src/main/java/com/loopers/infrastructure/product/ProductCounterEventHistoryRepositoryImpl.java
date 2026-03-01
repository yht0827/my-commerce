package com.loopers.infrastructure.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.loopers.domain.product.ProductCounterEventHistory;
import com.loopers.domain.product.ProductCounterEventHistoryRepository;
import com.loopers.domain.product.ProductCounterProcessStatus;
import com.loopers.domain.product.ProductCounterType;
import com.loopers.domain.product.ProductId;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductCounterEventHistoryRepositoryImpl implements ProductCounterEventHistoryRepository {

	private final ProductCounterEventHistoryJpaRepository productCounterEventHistoryJpaRepository;

	@Override
	public boolean createIfNotExists(final String dedupeKey, final ProductId productId, final ProductCounterType counterType) {
		return productCounterEventHistoryJpaRepository.createIfNotExists(
			dedupeKey,
			productId.getProductId(),
			counterType.name()) > 0;
	}

	@Override
	public Optional<ProductCounterEventHistory> findByDedupeKey(final String dedupeKey) {
		return productCounterEventHistoryJpaRepository.findByDedupeKey(dedupeKey);
	}

	@Override
	public long countCompletedByProductIdAndCounterType(final ProductId productId, final ProductCounterType counterType) {
		return productCounterEventHistoryJpaRepository.countByProductIdAndCounterTypeAndProcessStatus(
			productId.getProductId(),
			counterType,
			ProductCounterProcessStatus.COMPLETED);
	}

	@Override
	public List<ProductCounterEventHistory> findFailedEvents(final int limit) {
		int maxLimit = Math.max(1, limit);
		return productCounterEventHistoryJpaRepository.findByProcessStatusOrderByUpdatedAtAsc(
			ProductCounterProcessStatus.FAILED,
			PageRequest.of(0, maxLimit));
	}
}
