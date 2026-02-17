package com.loopers.domain.product;

import java.util.Optional;

public interface ProductAggregateRepository {

	Optional<ProductAggregate> findByProductId(final ProductId productId);

	Optional<ProductAggregate> findByProductIdWithOptimisticLock(final ProductId productId);

	Optional<ProductAggregate> findByProductIdWithPessimisticLock(final ProductId productId);

	Optional<ProductAggregate> findById(final Long id);

	ProductAggregate save(final ProductAggregate productAggregate);

	boolean incrementLikeCount(final ProductId productId);

	boolean decrementLikeCount(final ProductId productId);

	boolean existsByProductId(final ProductId productId);
}
