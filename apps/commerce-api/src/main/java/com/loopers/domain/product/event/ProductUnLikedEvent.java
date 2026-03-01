package com.loopers.domain.product.event;

import java.time.LocalDateTime;

public record ProductUnLikedEvent(String userId, Long productId, LocalDateTime occurredAt) implements ProductEvent {

	public static final String EVENT_TYPE = "PRODUCT_UNLIKED";

	public static ProductUnLikedEvent create(String userId, Long productId) {
		return new ProductUnLikedEvent(userId, productId, LocalDateTime.now());
	}

	public String getUserId() {
		return userId;
	}

	@Override
	public String getAggregateId() {
		return userId;
	}

	@Override
	public LocalDateTime getOccurredAt() {
		return occurredAt;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public String getCorrelationId() {
		return EVENT_TYPE + ":" + userId + ":" + productId + ":" + occurredAt;
	}

	@Override
	public Long getProductId() {
		return productId;
	}
}
