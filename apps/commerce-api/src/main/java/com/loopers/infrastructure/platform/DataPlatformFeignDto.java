package com.loopers.infrastructure.platform;

import java.time.LocalDateTime;

import com.loopers.domain.platform.event.DataPlatformEvent;

public record DataPlatformFeignDto() {

	public record EventRequest(
		String dataType,
		String aggregateId,
		LocalDateTime occurredAt,
		String correlationId
	) {
		public static EventRequest from(final DataPlatformEvent event) {
			return new EventRequest(
				event.dataType(),
				event.aggregateId(),
				event.occurredAt(),
				event.getCorrelationId()
			);
		}
	}
}
