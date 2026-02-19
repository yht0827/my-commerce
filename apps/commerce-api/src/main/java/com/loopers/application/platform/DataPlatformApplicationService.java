package com.loopers.application.platform;

import org.springframework.stereotype.Service;

import com.loopers.domain.platform.DataPlatformGateway;
import com.loopers.domain.platform.event.DataPlatformEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DataPlatformApplicationService {

	private final DataPlatformGateway dataPlatformGateway;

	public void sendEvent(final DataPlatformEvent event) {
		try {
			dataPlatformGateway.send(event);
		} catch (Exception e) {
			log.error(
				"Data platform dispatch failed: eventType={}, dataType={}, aggregateId={}, occurredAt={}, correlationId={}",
				event.getEventType(),
				event.dataType(),
				event.aggregateId(),
				event.occurredAt(),
				event.getCorrelationId(),
				e
			);
		}
	}
}
