package com.loopers.domain.stock;

import org.springframework.stereotype.Service;

import com.loopers.domain.common.Quantity;
import com.loopers.domain.product.ProductId;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockService {

	private final StockRepository stockRepository;

	public Stock syncFromProductQuantity(final ProductId productId, final Quantity quantity) {
		Stock stock = stockRepository.findByProductIdWithPessimisticLock(productId)
			.orElseGet(() -> stockRepository.save(new Stock(productId, quantity)));
		stock.changeQuantity(quantity);
		return stock;
	}
}
