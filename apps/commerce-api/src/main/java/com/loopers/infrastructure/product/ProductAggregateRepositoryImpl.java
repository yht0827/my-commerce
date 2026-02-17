package com.loopers.infrastructure.product;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.loopers.domain.product.ProductAggregate;
import com.loopers.domain.product.ProductAggregateRepository;
import com.loopers.domain.product.ProductId;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductAggregateRepositoryImpl implements ProductAggregateRepository {

	private final ProductAggregateJpaRepository productAggregateJpaRepository;

	@Override
	public Optional<ProductAggregate> findByProductId(final ProductId productId) {
		return productAggregateJpaRepository.findByProductId(productId.getProductId());
	}

	@Override
	public Optional<ProductAggregate> findByProductIdWithOptimisticLock(final ProductId productId) {
		return productAggregateJpaRepository.findByIdWithOptimisticLock(productId.getProductId());
	}

	@Override
	public Optional<ProductAggregate> findByProductIdWithPessimisticLock(final ProductId productId) {
		return productAggregateJpaRepository.findByIdWithPessimisticLock(productId.getProductId());
	}

	@Override
	public Optional<ProductAggregate> findById(final Long id) {
		return productAggregateJpaRepository.findById(id);
	}

	@Override
	public ProductAggregate save(final ProductAggregate productAggregate) {
		return productAggregateJpaRepository.save(productAggregate);
	}

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
	public boolean existsByProductId(final ProductId productId) {
		return productAggregateJpaRepository.existsByProductId(productId.getProductId());
	}

}
