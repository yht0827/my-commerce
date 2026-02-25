package com.loopers.infrastructure.product;

import java.util.List;

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
	public boolean incrementOrderCount(final ProductId productId) {
		int updatedCount = productAggregateJpaRepository.incrementOrderCount(productId.getProductId());
		return updatedCount > 0;
	}

	@Override
	public boolean incrementViewCount(final ProductId productId) {
		int updatedCount = productAggregateJpaRepository.incrementViewCount(productId.getProductId());
		return updatedCount > 0;
	}

	@Override
	public List<ProductId> findAllProductIds() {
		return productAggregateJpaRepository.findAllProductIds().stream()
			.map(ProductId::of)
			.toList();
	}

	@Override
	public void replaceCounts(final ProductId productId, final long likeCount, final long orderCount, final long viewCount) {
		productAggregateJpaRepository.replaceCounts(productId.getProductId(), likeCount, orderCount, viewCount);
	}

	@Override
	public void createIfNotExists(final ProductId productId) {
		productAggregateJpaRepository.createIfNotExists(productId.getProductId());
	}
}
