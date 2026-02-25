package com.loopers.application.product;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.loopers.domain.product.ProductAggregateService;
import com.loopers.domain.product.ProductCacheInvalidationService;
import com.loopers.domain.product.ProductCounterType;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.product.event.ProductOrderedEvent;
import com.loopers.domain.product.event.ProductViewedEvent;
import com.loopers.support.event.Envelope;
import com.loopers.application.ranking.RankingRealtimeUpdateService;

@DisplayName("ProductCounterEventHandler 테스트")
class ProductCounterEventHandlerTest {

	private final ProductCounterEventHistoryService productCounterEventHistoryService =
		mock(ProductCounterEventHistoryService.class);
	private final ProductAggregateService productAggregateService = mock(ProductAggregateService.class);
	private final ProductCacheInvalidationService productCacheInvalidationService = mock(ProductCacheInvalidationService.class);
	private final RankingRealtimeUpdateService rankingRealtimeUpdateService = mock(RankingRealtimeUpdateService.class);

	private final ProductCounterEventHandler productCounterEventHandler = new ProductCounterEventHandler(
		productCounterEventHistoryService,
		productAggregateService,
		productCacheInvalidationService,
		rankingRealtimeUpdateService
	);

	@Test
	@DisplayName("주문 이벤트는 dedupe claim 성공 시 order_count를 증가시키고 완료 처리한다")
	void handleProductOrdered_incrementsAndCompletes() {
		ProductOrderedEvent event = ProductOrderedEvent.create("ORD-1001", 10L);
		Envelope<ProductOrderedEvent> envelope = Envelope.of(event);
		ProductId productId = ProductId.of(10L);

		when(productCounterEventHistoryService.generateDedupeKey(anyString())).thenReturn("dedupe-order-1");
		when(productCounterEventHistoryService.claim("dedupe-order-1", productId, ProductCounterType.ORDER)).thenReturn(true);
		when(productAggregateService.incrementOrderCount(productId)).thenReturn(true);

		productCounterEventHandler.handleProductOrdered(envelope);

		verify(productCounterEventHistoryService).markProcessing("dedupe-order-1");
		verify(productAggregateService).incrementOrderCount(productId);
		verify(productCounterEventHistoryService).complete("dedupe-order-1");
		verify(productCacheInvalidationService).evictProductCache(productId);
		verify(rankingRealtimeUpdateService).incrementOrder(eq(10L), any());
	}

	@Test
	@DisplayName("주문 이벤트가 중복이면 카운터 업데이트를 건너뛴다")
	void handleProductOrdered_skipsWhenDuplicate() {
		ProductOrderedEvent event = ProductOrderedEvent.create("ORD-1001", 10L);
		Envelope<ProductOrderedEvent> envelope = Envelope.of(event);
		ProductId productId = ProductId.of(10L);

		when(productCounterEventHistoryService.generateDedupeKey(anyString())).thenReturn("dedupe-order-dup");
		when(productCounterEventHistoryService.claim("dedupe-order-dup", productId, ProductCounterType.ORDER)).thenReturn(false);

		productCounterEventHandler.handleProductOrdered(envelope);

		verify(productAggregateService, never()).incrementOrderCount(any());
		verify(productCounterEventHistoryService, never()).markProcessing(anyString());
		verify(productCounterEventHistoryService, never()).complete(anyString());
	}

	@Test
	@DisplayName("조회 이벤트는 aggregate가 없으면 생성 후 view_count 증가를 재시도한다")
	void handleProductViewed_createsAggregateAndRetries() {
		ProductViewedEvent event = ProductViewedEvent.create(11L);
		Envelope<ProductViewedEvent> envelope = Envelope.of(event);
		ProductId productId = ProductId.of(11L);

		when(productCounterEventHistoryService.generateDedupeKey(anyString())).thenReturn("dedupe-view-1");
		when(productCounterEventHistoryService.claim("dedupe-view-1", productId, ProductCounterType.VIEW)).thenReturn(true);
		when(productAggregateService.incrementViewCount(productId)).thenReturn(false, true);

		productCounterEventHandler.handleProductViewed(envelope);

		verify(productCounterEventHistoryService).markProcessing("dedupe-view-1");
		verify(productAggregateService, times(2)).incrementViewCount(productId);
		verify(productAggregateService).createIfNotExists(productId);
		verify(productCounterEventHistoryService).complete("dedupe-view-1");
		verify(productCacheInvalidationService).evictProductCache(productId);
		verify(rankingRealtimeUpdateService).incrementView(eq(11L), any());
	}
}
