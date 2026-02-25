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
			// 일반 경로는 예외를 삼키고 로그만 남긴다(비차단 전송).
			sendEventWithFailure(event);
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

	public void sendEventWithFailure(final DataPlatformEvent event) {
		// outbox dispatcher 경로는 실패를 상위로 전파해 retry 제어를 맡긴다.
		dataPlatformGateway.send(event);
	}
}
