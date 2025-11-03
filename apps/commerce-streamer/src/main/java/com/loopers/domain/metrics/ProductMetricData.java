package com.loopers.domain.metrics;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProductMetricData(
	String eventId,
	Long productId,
	String action, // "LIKE" or "UNLIKE"
	String userId,
	LocalDate eventDate,
	LocalDateTime occurredAt,
	Long version
) {

	public static ProductMetricData of(String eventId, Long productId, String action,
		String userId, LocalDate eventDate, LocalDateTime occurredAt, Long version) {
		return new ProductMetricData(eventId, productId, action, userId, eventDate, occurredAt, version);
	}

	public Long getLikeChange() {
		return "LIKE".equals(action) ? 1L : -1L;
	}
}
