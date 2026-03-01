package com.loopers.infrastructure.platform;

import org.springframework.stereotype.Component;

import com.loopers.domain.platform.DataPlatformGateway;
import com.loopers.domain.platform.event.DataPlatformEvent;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DataPlatformFeignClient implements DataPlatformGateway {

	@Override
	public void send(final DataPlatformEvent event) {
		// TODO: Replace this stub with real external call and apply @Retry/@CircuitBreaker on this boundary.
		log.info(
			"Data platform dispatch stub: eventType={}, dataType={}, aggregateId={}, occurredAt={}, correlationId={}",
			event.getEventType(),
			event.dataType(),
			event.aggregateId(),
			event.occurredAt(),
			event.getCorrelationId()
		);
	}
}
