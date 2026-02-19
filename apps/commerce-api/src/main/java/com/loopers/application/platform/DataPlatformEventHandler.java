package com.loopers.application.platform;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.domain.platform.event.DataPlatformEvent;
import com.loopers.support.event.Envelope;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataPlatformEventHandler {

	private final DataPlatformApplicationService dataPlatformApplicationService;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleDataPlatformEvent(Envelope<DataPlatformEvent> event) {
		if (!DataPlatformEvent.EVENT_TYPE.equals(event.getEventType())) {
			return;
		}

		dataPlatformApplicationService.sendEvent(event.getPayload());
	}
}
