package com.loopers.domain.stock;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import org.springframework.stereotype.Service;

import com.loopers.domain.common.Quantity;
import com.loopers.domain.product.ProductId;
import com.loopers.support.error.CoreException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockService {

	private final StockRepository stockRepository;

	public Stock deduct(final ProductId productId, final Quantity amount) {
		Stock stock = findByProductIdWithPessimisticLock(productId);
		stock.deduct(amount);
		return stock;
	}

	public Stock increase(final ProductId productId, final Quantity amount) {
		Stock stock = findByProductIdWithPessimisticLock(productId);
		stock.increase(amount);
		return stock;
	}

	public Stock findByProductId(final ProductId productId) {
		return stockRepository.findByProductId(productId)
			.orElseThrow(() -> new CoreException(NOT_FOUND, STOCK_NOT_FOUND.format(productId.getProductId())));
	}

	public Stock findByProductIdWithPessimisticLock(final ProductId productId) {
		return stockRepository.findByProductIdWithPessimisticLock(productId)
			.orElseThrow(() -> new CoreException(NOT_FOUND, STOCK_NOT_FOUND.format(productId.getProductId())));
	}

	public Stock create(final ProductId productId, final Quantity quantity) {
		Stock stock = new Stock(productId, quantity);
		return stockRepository.save(stock);
	}

	public Stock syncQuantity(final ProductId productId, final Quantity quantity) {
		Stock stock = stockRepository.findByProductIdWithPessimisticLock(productId)
			.orElseGet(() -> stockRepository.save(new Stock(productId, quantity)));
		stock.changeQuantity(quantity);
		return stock;
	}
}
