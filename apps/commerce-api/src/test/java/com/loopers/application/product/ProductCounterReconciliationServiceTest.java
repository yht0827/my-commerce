package com.loopers.application.product;

import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.order.OrderItemRepository;
import com.loopers.domain.product.ProductAggregateRepository;
import com.loopers.domain.product.ProductCounterEventHistory;
import com.loopers.domain.product.ProductCounterEventHistoryRepository;
import com.loopers.domain.product.ProductCounterProcessStatus;
import com.loopers.domain.product.ProductCounterType;
import com.loopers.domain.product.ProductId;

@DisplayName("ProductCounterReconciliationService 테스트")
class ProductCounterReconciliationServiceTest {

	private final ProductAggregateRepository productAggregateRepository = mock(ProductAggregateRepository.class);
	private final LikeRepository likeRepository = mock(LikeRepository.class);
	private final OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
	private final ProductCounterEventHistoryRepository productCounterEventHistoryRepository =
		mock(ProductCounterEventHistoryRepository.class);
	private final ProductCounterEventHistoryService productCounterEventHistoryService =
		mock(ProductCounterEventHistoryService.class);

	private final ProductCounterReconciliationService productCounterReconciliationService =
		new ProductCounterReconciliationService(
			productAggregateRepository,
			likeRepository,
			orderItemRepository,
			productCounterEventHistoryRepository,
			productCounterEventHistoryService
		);

	@Test
	@DisplayName("전체 리컨실리에이션은 product_aggregate 기준으로 카운터를 다시 계산해 반영한다")
	void reconcileAllAggregates_replacesCountsBySources() {
		ProductId first = ProductId.of(1L);
		ProductId second = ProductId.of(2L);

		when(productAggregateRepository.findAllProductIds()).thenReturn(List.of(first, second));

		when(likeRepository.countByProductId(first)).thenReturn(10L);
		when(orderItemRepository.countConfirmedOrdersByProductId(first)).thenReturn(3L);
		when(productCounterEventHistoryRepository.countCompletedByProductIdAndCounterType(first, ProductCounterType.VIEW))
			.thenReturn(100L);

		when(likeRepository.countByProductId(second)).thenReturn(4L);
		when(orderItemRepository.countConfirmedOrdersByProductId(second)).thenReturn(7L);
		when(productCounterEventHistoryRepository.countCompletedByProductIdAndCounterType(second, ProductCounterType.VIEW))
			.thenReturn(22L);

		productCounterReconciliationService.reconcileAllAggregates();

		verify(productAggregateRepository).replaceCounts(first, 10L, 3L, 100L);
		verify(productAggregateRepository).replaceCounts(second, 4L, 7L, 22L);
	}

	@Test
	@DisplayName("실패 이력 재처리는 markProcessing -> 리컨실리에이션 -> complete 순서로 동작한다")
	void retryFailedCounterEvents_reconcilesAndCompletes() {
		ProductCounterEventHistory failedEvent = ProductCounterEventHistory.create()
			.dedupeKey("failed-dedupe-1")
			.productId(ProductId.of(10L))
			.counterType(ProductCounterType.VIEW)
			.processStatus(ProductCounterProcessStatus.FAILED)
			.build();

		when(productCounterEventHistoryRepository.findFailedEvents(50)).thenReturn(List.of(failedEvent));
		when(likeRepository.countByProductId(ProductId.of(10L))).thenReturn(2L);
		when(orderItemRepository.countConfirmedOrdersByProductId(ProductId.of(10L))).thenReturn(1L);
		when(productCounterEventHistoryRepository.countCompletedByProductIdAndCounterType(ProductId.of(10L), ProductCounterType.VIEW))
			.thenReturn(13L);

		productCounterReconciliationService.retryFailedCounterEvents(50);

		verify(productCounterEventHistoryService).markProcessing("failed-dedupe-1");
		verify(productAggregateRepository).replaceCounts(ProductId.of(10L), 2L, 1L, 13L);
		verify(productCounterEventHistoryService).complete("failed-dedupe-1");
		verify(productCounterEventHistoryService, never()).fail(eq("failed-dedupe-1"), anyString());
	}
}
