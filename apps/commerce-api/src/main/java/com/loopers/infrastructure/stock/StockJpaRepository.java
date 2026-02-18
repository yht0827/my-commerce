package com.loopers.infrastructure.stock;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.loopers.domain.product.ProductId;
import com.loopers.domain.stock.Stock;

import jakarta.persistence.LockModeType;

public interface StockJpaRepository extends JpaRepository<Stock, Long> {

	@Query("SELECT s FROM Stock s WHERE s.productId = :productId")
	Optional<Stock> findByProductId(final ProductId productId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM Stock s WHERE s.productId = :productId")
	Optional<Stock> findByProductIdWithPessimisticLock(final ProductId productId);

	@Lock(LockModeType.OPTIMISTIC)
	@Query("SELECT s FROM Stock s WHERE s.productId = :productId")
	Optional<Stock> findByProductIdWithOptimisticLock(final ProductId productId);
}
