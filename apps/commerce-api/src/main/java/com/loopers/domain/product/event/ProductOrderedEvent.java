package com.loopers.domain.product.event;

import java.time.LocalDateTime;

public record ProductOrderedEvent(
	String orderId,
	Long productId,
	LocalDateTime occurredAt
) implements ProductEvent {

	public static final String EVENT_TYPE = "PRODUCT_ORDERED";

	public static ProductOrderedEvent create(final String orderId, final Long productId) {
		return new ProductOrderedEvent(orderId, productId, LocalDateTime.now());
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
		return orderId;
	}

	@Override
	public Long getProductId() {
		return productId;
	}
}
