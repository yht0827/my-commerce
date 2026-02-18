package com.loopers.domain.stock;

import java.util.Optional;

import com.loopers.domain.product.ProductId;

public interface StockRepository {

	Optional<Stock> findByProductId(final ProductId productId);

	Optional<Stock> findByProductIdWithPessimisticLock(final ProductId productId);

	Optional<Stock> findByProductIdWithOptimisticLock(final ProductId productId);

	Stock save(final Stock stock);
}
