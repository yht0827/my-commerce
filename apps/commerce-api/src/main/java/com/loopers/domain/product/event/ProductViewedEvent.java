package com.loopers.domain.product.event;

import java.time.LocalDateTime;

public record ProductViewedEvent(
	Long productId,
	LocalDateTime occurredAt
) implements ProductEvent {

	public static final String EVENT_TYPE = "PRODUCT_VIEWED";

	public static ProductViewedEvent create(final Long productId) {
		return new ProductViewedEvent(productId, LocalDateTime.now());
	}

	@Override
	public String getAggregateId() {
		return String.valueOf(productId);
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
		return EVENT_TYPE + ":" + productId + ":" + occurredAt;
	}

	@Override
	public Long getProductId() {
		return productId;
	}
}
