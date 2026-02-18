package com.loopers.domain.stock;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.loopers.domain.common.Quantity;
import com.loopers.domain.product.ProductId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("재고 도메인 테스트")
public class StockTest {

	@DisplayName("재고를 생성할 때")
	@Nested
	class Create {

		@DisplayName("초기 수량이 null이면 0으로 생성된다")
		@Test
		void createStockWithZero_whenQuantityIsNull() {
			// arrange
			ProductId productId = ProductId.of(1L);

			// act
			Stock stock = new Stock(productId, null);

			// assert
			assertThat(stock.getProductId()).isEqualTo(productId);
			assertThat(stock.getQuantity().getQuantity()).isEqualTo(0L);
		}
	}

	@DisplayName("재고를 차감할 때")
	@Nested
	class Deduct {

		@DisplayName("현재 재고보다 많이 차감하면 예외가 발생한다")
		@Test
		void failWhenDeductingMoreThanCurrentQuantity() {
			// arrange
			Stock stock = new Stock(ProductId.of(1L), new Quantity(3L));

			// act
			CoreException result = assertThrows(CoreException.class, () -> stock.deduct(new Quantity(4L)));

			// assert
			assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
		}

		@DisplayName("정상 차감이면 수량이 감소한다")
		@Test
		void deductQuantity_whenStockIsSufficient() {
			// arrange
			Stock stock = new Stock(ProductId.of(1L), new Quantity(10L));

			// act
			stock.deduct(new Quantity(4L));

			// assert
			assertThat(stock.getQuantity().getQuantity()).isEqualTo(6L);
			assertThat(stock.isOutOfStock()).isFalse();
		}

		@DisplayName("전체 수량을 차감하면 품절 상태가 된다")
		@Test
		void becomeOutOfStock_whenAllQuantityDeducted() {
			// arrange
			Stock stock = new Stock(ProductId.of(1L), new Quantity(5L));

			// act
			stock.deduct(new Quantity(5L));

			// assert
			assertThat(stock.getQuantity().getQuantity()).isEqualTo(0L);
			assertThat(stock.isOutOfStock()).isTrue();
		}
	}

	@DisplayName("재고를 증가시킬 때")
	@Nested
	class Increase {

		@DisplayName("증가 수량만큼 재고가 증가한다")
		@Test
		void increaseQuantity() {
			// arrange
			Stock stock = new Stock(ProductId.of(1L), new Quantity(2L));

			// act
			stock.increase(new Quantity(3L));

			// assert
			assertThat(stock.getQuantity().getQuantity()).isEqualTo(5L);
		}
	}
}
