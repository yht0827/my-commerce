package com.loopers.domain.product.event;

import java.time.LocalDateTime;

public record ProductLikedEvent(
	String userId,
	Long productId,
	LocalDateTime occurredAt
) implements ProductEvent {

	public static final String EVENT_TYPE = "PRODUCT_LIKED";

	public static ProductLikedEvent create(String userId, Long productId) {
		return new ProductLikedEvent(userId, productId, LocalDateTime.now());
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

	public String getUserId() {
		return userId;
	}

	@Override
	public Long getProductId() {
		return productId;
	}
}
