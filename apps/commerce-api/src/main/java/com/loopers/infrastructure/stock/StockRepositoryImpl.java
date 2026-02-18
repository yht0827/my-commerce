package com.loopers.infrastructure.stock;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.loopers.domain.product.ProductId;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class StockRepositoryImpl implements StockRepository {

	private final StockJpaRepository stockJpaRepository;

	@Override
	public Optional<Stock> findByProductId(final ProductId productId) {
		return stockJpaRepository.findByProductId(productId);
	}

	@Override
	public Optional<Stock> findByProductIdWithPessimisticLock(final ProductId productId) {
		return stockJpaRepository.findByProductIdWithPessimisticLock(productId);
	}

	@Override
	public Optional<Stock> findByProductIdWithOptimisticLock(final ProductId productId) {
		return stockJpaRepository.findByProductIdWithOptimisticLock(productId);
	}

	@Override
	public Stock save(final Stock stock) {
		return stockJpaRepository.save(stock);
	}
}
