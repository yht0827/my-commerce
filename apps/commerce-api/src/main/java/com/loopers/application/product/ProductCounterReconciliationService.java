package com.loopers.application.product;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.order.OrderItemRepository;
import com.loopers.domain.product.ProductAggregateRepository;
import com.loopers.domain.product.ProductCounterEventHistory;
import com.loopers.domain.product.ProductCounterEventHistoryRepository;
import com.loopers.domain.product.ProductCounterType;
import com.loopers.domain.product.ProductId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductCounterReconciliationService {

	private final ProductAggregateRepository productAggregateRepository;
	private final LikeRepository likeRepository;
	private final OrderItemRepository orderItemRepository;
	private final ProductCounterEventHistoryRepository productCounterEventHistoryRepository;
	private final ProductCounterEventHistoryService productCounterEventHistoryService;

	@Transactional
	public void reconcileAllAggregates() {
		List<ProductId> productIds = productAggregateRepository.findAllProductIds();
		for (ProductId productId : productIds) {
			reconcileProduct(productId);
		}
		log.info("상품 카운터 리컨실리에이션 완료. productCount={}", productIds.size());
	}

	@Transactional
	public void retryFailedCounterEvents(final int limit) {
		List<ProductCounterEventHistory> failedEvents = productCounterEventHistoryRepository.findFailedEvents(limit);
		for (ProductCounterEventHistory failedEvent : failedEvents) {
			String dedupeKey = failedEvent.getDedupeKey();
			Long productId = failedEvent.getProductId().getProductId();
			try {
				productCounterEventHistoryService.markProcessing(dedupeKey);
				reconcileProduct(failedEvent.getProductId());
				productCounterEventHistoryService.complete(dedupeKey);
			} catch (Exception e) {
				productCounterEventHistoryService.fail(dedupeKey, e.getMessage());
				log.error("실패 카운터 이벤트 재처리 실패. dedupeKey={}, productId={}", dedupeKey, productId, e);
			}
		}

		if (!failedEvents.isEmpty()) {
			log.info("실패 카운터 이벤트 재처리 완료. retriedCount={}", failedEvents.size());
		}
	}

	@Transactional
	public void reconcileProduct(final ProductId productId) {
		long likeCount = likeRepository.countByProductId(productId);
		long orderCount = orderItemRepository.countConfirmedOrdersByProductId(productId);
		long viewCount = productCounterEventHistoryRepository.countCompletedByProductIdAndCounterType(productId,
			ProductCounterType.VIEW);

		productAggregateRepository.createIfNotExists(productId);
		productAggregateRepository.replaceCounts(productId, likeCount, orderCount, viewCount);
	}
}
