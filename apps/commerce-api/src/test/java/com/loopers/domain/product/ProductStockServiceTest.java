package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.loopers.domain.brand.BrandId;
import com.loopers.domain.common.Price;
import com.loopers.domain.common.Quantity;
import com.loopers.domain.order.OrderData;
import com.loopers.domain.order.OrderId;
import com.loopers.domain.order.OrderItem;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("ProductStockService 단위 테스트")
class ProductStockServiceTest {

	private final ProductRepository productRepository = mock(ProductRepository.class);
	private final ProductStockService productStockService = new ProductStockService(productRepository);

	private Product createProduct(long quantity, ProductStatus status) {
		return Product.builder()
			.brandId(new BrandId(1L))
			.name(new ProductName("테스트 상품"))
			.price(new Price(10000L))
			.quantity(new Quantity(quantity))
			.status(status)
			.build();
	}

	@DisplayName("deductStock 시")
	@Nested
	class DeductStock {

		@DisplayName("null 리스트이면 빈 결과를 반환한다")
		@Test
		void deductStock_returnsEmpty_whenOrderItemsIsNull() {
			List<ProductData.StockQuantityChanged> result = productStockService.deductStock(null);

			assertThat(result).isEmpty();
			verifyNoInteractions(productRepository);
		}

		@DisplayName("빈 리스트이면 빈 결과를 반환한다")
		@Test
		void deductStock_returnsEmpty_whenOrderItemsIsEmpty() {
			// act
			List<ProductData.StockQuantityChanged> result = productStockService.deductStock(List.of());

			// assert
			assertThat(result).isEmpty();
			verifyNoInteractions(productRepository);
		}

		@DisplayName("정상 차감 후 변경된 수량 정보를 반환한다")
		@Test
		void deductStock_returnsQuantityChanged_afterDeduction() {
			// arrange
			Product product = createProduct(10L, ProductStatus.ON_SALE);
			when(productRepository.findByIdWithPessimisticLock(any())).thenReturn(Optional.of(product));

			List<OrderData.OrderItemData> orderItems = List.of(
				new OrderData.OrderItemData(1L, 3L)
			);

			// act
			List<ProductData.StockQuantityChanged> result = productStockService.deductStock(orderItems);

			// assert
			assertThat(result).hasSize(1);
			assertThat(result.get(0).productId()).isEqualTo(1L);
			assertThat(result.get(0).previousQuantity()).isEqualTo(10L);
			assertThat(result.get(0).currentQuantity()).isEqualTo(7L);
		}

		@DisplayName("존재하지 않는 상품이면 CoreException(NOT_FOUND)이 발생한다")
		@Test
		void deductStock_throwsCoreException_whenProductNotFound() {
			// arrange
			when(productRepository.findByIdWithPessimisticLock(any())).thenReturn(Optional.empty());

			List<OrderData.OrderItemData> orderItems = List.of(
				new OrderData.OrderItemData(999L, 1L)
			);

			// act & assert
			CoreException exception = assertThrows(
				CoreException.class,
				() -> productStockService.deductStock(orderItems)
			);

			assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
		}

		@DisplayName("null/미완성 항목은 무시하고 같은 상품은 수량을 합산해 한 번만 차감한다")
		@Test
		void deductStock_skipsInvalidItemsAndMergesDuplicatedProductItems() {
			Product product = createProduct(20L, ProductStatus.ON_SALE);
			when(productRepository.findByIdWithPessimisticLock(ProductId.of(1L))).thenReturn(Optional.of(product));

			List<OrderData.OrderItemData> orderItems = Arrays.asList(
				null,
				new OrderData.OrderItemData(null, 1L),
				new OrderData.OrderItemData(1L, null),
				new OrderData.OrderItemData(1L, 2L),
				new OrderData.OrderItemData(1L, 3L)
			);

			List<ProductData.StockQuantityChanged> result = productStockService.deductStock(orderItems);

			assertThat(result).hasSize(1);
			assertThat(result.get(0).previousQuantity()).isEqualTo(20L);
			assertThat(result.get(0).currentQuantity()).isEqualTo(15L);
			verify(productRepository, times(1)).findByIdWithPessimisticLock(ProductId.of(1L));
		}
	}

	@DisplayName("restoreStock 시")
	@Nested
	class RestoreStock {

		@DisplayName("null 리스트이면 아무것도 하지 않는다")
		@Test
		void restoreStock_doesNothing_whenOrderItemsIsNull() {
			productStockService.restoreStock(null);

			verifyNoInteractions(productRepository);
		}

		@DisplayName("빈 리스트이면 아무것도 하지 않는다")
		@Test
		void restoreStock_doesNothing_whenOrderItemsIsEmpty() {
			// act
			productStockService.restoreStock(List.of());

			// assert
			verifyNoInteractions(productRepository);
		}

		@DisplayName("정상적으로 재고를 복구한다")
		@Test
		void restoreStock_restoresProductStock() {
			// arrange
			Product product = createProduct(5L, ProductStatus.ON_SALE);
			when(productRepository.findByIdWithPessimisticLock(any())).thenReturn(Optional.of(product));

			List<OrderItem> orderItems = List.of(
				OrderItem.builder()
					.orderId(new OrderId("ORD-1"))
					.productId(ProductId.of(1L))
					.quantity(new Quantity(2L))
					.price(new Price(1000L))
					.build()
			);

			// act
			productStockService.restoreStock(orderItems);

			// assert
			assertThat(product.getQuantity().getQuantity()).isEqualTo(7L);
		}
	}
}
