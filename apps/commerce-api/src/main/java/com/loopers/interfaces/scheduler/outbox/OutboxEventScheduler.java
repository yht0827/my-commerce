package com.loopers.interfaces.scheduler.outbox;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.loopers.application.outbox.OutboxDispatcherService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {

	private final OutboxDispatcherService outboxDispatcherService;

	@Scheduled(fixedDelay = 1000)
	public void dispatchPendingEvents() {
		// 주문 커밋 이후 적재된 outbox 이벤트를 비동기로 순차 처리한다.
		outboxDispatcherService.dispatchPendingEvents();
	}
}
