package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProductAggregateService 단위 테스트")
class ProductAggregateServiceTest {

	private final ProductAggregateRepository productAggregateRepository = mock(ProductAggregateRepository.class);
	private final ProductAggregateService productAggregateService = new ProductAggregateService(productAggregateRepository);

	@DisplayName("incrementLikeCount - repository에 위임하고 결과를 반환한다")
	@Test
	void incrementLikeCount_delegatesToRepository_andReturnsResult() {
		// arrange
		ProductId productId = ProductId.of(1L);
		when(productAggregateRepository.incrementLikeCount(productId)).thenReturn(true);

		// act
		boolean result = productAggregateService.incrementLikeCount(productId);

		// assert
		assertThat(result).isTrue();
		verify(productAggregateRepository, times(1)).incrementLikeCount(productId);
	}

	@DisplayName("decrementLikeCount - repository에 위임하고 결과를 반환한다")
	@Test
	void decrementLikeCount_delegatesToRepository_andReturnsResult() {
		// arrange
		ProductId productId = ProductId.of(1L);
		when(productAggregateRepository.decrementLikeCount(productId)).thenReturn(true);

		// act
		boolean result = productAggregateService.decrementLikeCount(productId);

		// assert
		assertThat(result).isTrue();
		verify(productAggregateRepository, times(1)).decrementLikeCount(productId);
	}

	@DisplayName("incrementOrderCount - repository에 위임하고 결과를 반환한다")
	@Test
	void incrementOrderCount_delegatesToRepository_andReturnsResult() {
		// arrange
		ProductId productId = ProductId.of(2L);
		when(productAggregateRepository.incrementOrderCount(productId)).thenReturn(true);

		// act
		boolean result = productAggregateService.incrementOrderCount(productId);

		// assert
		assertThat(result).isTrue();
		verify(productAggregateRepository, times(1)).incrementOrderCount(productId);
	}

	@DisplayName("incrementViewCount - repository에 위임하고 결과를 반환한다")
	@Test
	void incrementViewCount_delegatesToRepository_andReturnsResult() {
		// arrange
		ProductId productId = ProductId.of(3L);
		when(productAggregateRepository.incrementViewCount(productId)).thenReturn(true);

		// act
		boolean result = productAggregateService.incrementViewCount(productId);

		// assert
		assertThat(result).isTrue();
		verify(productAggregateRepository, times(1)).incrementViewCount(productId);
	}

	@DisplayName("createIfNotExists - repository에 위임한다")
	@Test
	void createIfNotExists_delegatesToRepository() {
		// arrange
		ProductId productId = ProductId.of(1L);

		// act
		productAggregateService.createIfNotExists(productId);

		// assert
		verify(productAggregateRepository, times(1)).createIfNotExists(productId);
	}
}
