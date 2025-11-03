package com.loopers.domain.event;

import java.time.LocalDateTime;

public record AuditLogData(
	String eventId,
	String eventType,
	String topic,
	Integer partition,
	Long offset,
	String payload,
	LocalDateTime occurredAt,
	Long version
) {

	public static AuditLogData of(String eventId, String eventType, String topic,
		Integer partition, Long offset, String payload,
		LocalDateTime occurredAt, Long version) {
		return new AuditLogData(eventId, eventType, topic, partition, offset,
			payload, occurredAt, version);
	}
}
