package com.loopers.domain.product.event;

import java.time.LocalDateTime;

public record ProductQuantityChangedEvent(
	Long productId,
	Long previousQuantity,
	Long currentQuantity,
	LocalDateTime occurredAt
) implements ProductEvent {

	public static final String EVENT_TYPE = "PRODUCT_QUANTITY_CHANGED";

	public static ProductQuantityChangedEvent create(
		final Long productId,
		final Long previousQuantity,
		final Long currentQuantity
	) {
		return new ProductQuantityChangedEvent(productId, previousQuantity, currentQuantity, LocalDateTime.now());
	}

	@Override
	public String getAggregateId() {
		return productId.toString();
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
