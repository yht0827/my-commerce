package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.loopers.domain.brand.BrandId;
import com.loopers.domain.common.Price;
import com.loopers.domain.common.Quantity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("Product 단위 테스트")
class ProductTest {

	private Product createProduct(long quantity, ProductStatus status) {
		return Product.builder()
			.brandId(new BrandId(1L))
			.name(new ProductName("테스트 상품"))
			.price(new Price(10000L))
			.quantity(new Quantity(quantity))
			.status(status)
			.build();
	}

	@DisplayName("재고 차감(deduct) 시")
	@Nested
	class Deduct {

		@DisplayName("정상적으로 재고를 차감한다")
		@Test
		void deductStock_success() {
			// arrange
			Product product = createProduct(10L, ProductStatus.ON_SALE);

			// act
			product.deduct(new Quantity(3L));

			// assert
			assertThat(product.getQuantity().getQuantity()).isEqualTo(7L);
			assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
		}

		@DisplayName("차감 후 재고가 0이면 SOLD_OUT으로 변경된다")
		@Test
		void deductStock_becomeSoldOut_whenQuantityReachesZero() {
			// arrange
			Product product = createProduct(3L, ProductStatus.ON_SALE);

			// act
			product.deduct(new Quantity(3L));

			// assert
			assertThat(product.getQuantity().getQuantity()).isEqualTo(0L);
			assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
		}

		@DisplayName("SOLD_OUT 상태에서 차감하면 CoreException(BAD_REQUEST)이 발생한다")
		@Test
		void deductStock_throwsCoreException_whenProductIsSoldOut() {
			// arrange
			Product product = createProduct(0L, ProductStatus.SOLD_OUT);

			// act & assert
			CoreException exception = assertThrows(
				CoreException.class,
				() -> product.deduct(new Quantity(1L))
			);

			assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
		}
	}

	@DisplayName("재고 복구(restore) 시")
	@Nested
	class Restore {

		@DisplayName("정상적으로 재고를 복구한다")
		@Test
		void restoreStock_success() {
			// arrange
			Product product = createProduct(5L, ProductStatus.ON_SALE);

			// act
			product.restore(new Quantity(3L));

			// assert
			assertThat(product.getQuantity().getQuantity()).isEqualTo(8L);
		}

		@DisplayName("SOLD_OUT 상태에서 복구 후 재고가 있으면 ON_SALE로 변경된다")
		@Test
		void restoreStock_becomeOnSale_whenRestoreFromSoldOut() {
			// arrange
			Product product = createProduct(0L, ProductStatus.SOLD_OUT);

			// act
			product.restore(new Quantity(5L));

			// assert
			assertThat(product.getQuantity().getQuantity()).isEqualTo(5L);
			assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
		}
	}
}
