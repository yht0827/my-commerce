package com.loopers.domain.stock;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.loopers.domain.common.Quantity;
import com.loopers.domain.product.ProductId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("재고 서비스 단위 테스트")
class StockServiceTest {

	@Test
	@DisplayName("재고가 없으면 차감 시 NOT_FOUND 예외가 발생한다")
	void throwsNotFound_whenStockDoesNotExist() {
		// arrange
		StockRepository stockRepository = mock(StockRepository.class);
		StockService stockService = new StockService(stockRepository);
		ProductId productId = ProductId.of(1L);
		when(stockRepository.findByProductIdWithPessimisticLock(productId)).thenReturn(Optional.empty());

		// act
		CoreException result = assertThrows(CoreException.class, () -> stockService.deduct(productId, new Quantity(1L)));

		// assert
		assertThat(result.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
	}

	@Test
	@DisplayName("재고가 있으면 정상적으로 차감된다")
	void deductsStock_whenStockExists() {
		// arrange
		StockRepository stockRepository = mock(StockRepository.class);
		StockService stockService = new StockService(stockRepository);
		ProductId productId = ProductId.of(1L);
		Stock stock = new Stock(productId, new Quantity(10L));
		when(stockRepository.findByProductIdWithPessimisticLock(productId)).thenReturn(Optional.of(stock));

		// act
		Stock result = stockService.deduct(productId, new Quantity(4L));

		// assert
		assertThat(result.getQuantity().getQuantity()).isEqualTo(6L);
	}

	@Test
	@DisplayName("syncQuantity 호출 시 재고가 없으면 생성한다")
	void createsStock_whenSyncTargetMissing() {
		// arrange
		StockRepository stockRepository = mock(StockRepository.class);
		StockService stockService = new StockService(stockRepository);
		ProductId productId = ProductId.of(1L);
		Quantity quantity = new Quantity(8L);
		Stock created = new Stock(productId, quantity);
		when(stockRepository.findByProductIdWithPessimisticLock(productId)).thenReturn(Optional.empty());
		when(stockRepository.save(any(Stock.class))).thenReturn(created);

		// act
		Stock result = stockService.syncQuantity(productId, quantity);

		// assert
		assertThat(result.getQuantity().getQuantity()).isEqualTo(8L);
		verify(stockRepository, times(1)).save(any(Stock.class));
	}
}
