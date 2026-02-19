package com.loopers.infrastructure.product;

import org.springframework.stereotype.Repository;

import com.loopers.domain.product.ProductAggregateRepository;
import com.loopers.domain.product.ProductId;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductAggregateRepositoryImpl implements ProductAggregateRepository {

	private final ProductAggregateJpaRepository productAggregateJpaRepository;

	@Override
	public boolean incrementLikeCount(final ProductId productId) {
		int updatedCount = productAggregateJpaRepository.incrementLikeCount(productId.getProductId());
		return updatedCount > 0;
	}

	@Override
	public boolean decrementLikeCount(final ProductId productId) {
		int updatedCount = productAggregateJpaRepository.decrementLikeCount(productId.getProductId());
		return updatedCount > 0;
	}

	@Override
	public void createIfNotExists(final ProductId productId) {
		productAggregateJpaRepository.createIfNotExists(productId.getProductId());
	}
}
