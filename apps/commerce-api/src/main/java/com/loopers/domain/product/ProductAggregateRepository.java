package com.loopers.domain.product;

public interface ProductAggregateRepository {

	boolean incrementLikeCount(final ProductId productId);

	boolean decrementLikeCount(final ProductId productId);

	void createIfNotExists(final ProductId productId);
}
