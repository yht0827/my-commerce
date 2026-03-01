package com.loopers.infrastructure.product;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.loopers.domain.product.ProductId;

@DisplayName("ProductAggregateRepositoryImpl 테스트")
class ProductAggregateRepositoryImplTest {

	private final ProductAggregateJpaRepository jpaRepository = mock(ProductAggregateJpaRepository.class);
	private final ProductAggregateRepositoryImpl repository = new ProductAggregateRepositoryImpl(jpaRepository);

	@Test
	@DisplayName("incrementLikeCount는 업데이트 건수가 0보다 크면 true를 반환한다")
	void incrementLikeCount_returnsTrueWhenUpdated() {
		when(jpaRepository.incrementLikeCount(1L)).thenReturn(1);

		boolean result = repository.incrementLikeCount(ProductId.of(1L));

		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("incrementLikeCount는 업데이트 건수가 0이면 false를 반환한다")
	void incrementLikeCount_returnsFalseWhenNotUpdated() {
		when(jpaRepository.incrementLikeCount(1L)).thenReturn(0);

		boolean result = repository.incrementLikeCount(ProductId.of(1L));

		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("decrementLikeCount는 업데이트 건수가 0보다 크면 true를 반환한다")
	void decrementLikeCount_returnsTrueWhenUpdated() {
		when(jpaRepository.decrementLikeCount(1L)).thenReturn(1);

		boolean result = repository.decrementLikeCount(ProductId.of(1L));

		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("decrementLikeCount는 업데이트 건수가 0이면 false를 반환한다")
	void decrementLikeCount_returnsFalseWhenNotUpdated() {
		when(jpaRepository.decrementLikeCount(1L)).thenReturn(0);

		boolean result = repository.decrementLikeCount(ProductId.of(1L));

		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("incrementOrderCount는 업데이트 건수가 0보다 크면 true를 반환한다")
	void incrementOrderCount_returnsTrueWhenUpdated() {
		when(jpaRepository.incrementOrderCount(1L)).thenReturn(2);

		boolean result = repository.incrementOrderCount(ProductId.of(1L));

		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("incrementOrderCount는 업데이트 건수가 0이면 false를 반환한다")
	void incrementOrderCount_returnsFalseWhenNotUpdated() {
		when(jpaRepository.incrementOrderCount(1L)).thenReturn(0);

		boolean result = repository.incrementOrderCount(ProductId.of(1L));

		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("incrementViewCount는 업데이트 건수가 0보다 크면 true를 반환한다")
	void incrementViewCount_returnsTrueWhenUpdated() {
		when(jpaRepository.incrementViewCount(1L)).thenReturn(1);

		boolean result = repository.incrementViewCount(ProductId.of(1L));

		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("incrementViewCount는 업데이트 건수가 0이면 false를 반환한다")
	void incrementViewCount_returnsFalseWhenNotUpdated() {
		when(jpaRepository.incrementViewCount(1L)).thenReturn(0);

		boolean result = repository.incrementViewCount(ProductId.of(1L));

		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("findAllProductIds는 조회된 Long 목록을 ProductId 목록으로 변환한다")
	void findAllProductIds_mapsIds() {
		when(jpaRepository.findAllProductIds()).thenReturn(List.of(1L, 2L, 3L));

		List<ProductId> ids = repository.findAllProductIds();

		assertThat(ids).containsExactly(ProductId.of(1L), ProductId.of(2L), ProductId.of(3L));
	}

	@Test
	@DisplayName("replaceCounts는 JPA 저장소에 count 교체를 위임한다")
	void replaceCounts_delegatesToJpaRepository() {
		repository.replaceCounts(ProductId.of(11L), 7L, 3L, 9L);

		verify(jpaRepository).replaceCounts(11L, 7L, 3L, 9L);
	}

	@Test
	@DisplayName("createIfNotExists는 JPA 저장소에 생성을 위임한다")
	void createIfNotExists_delegatesToJpaRepository() {
		repository.createIfNotExists(ProductId.of(21L));

		verify(jpaRepository).createIfNotExists(21L);
	}
}
