package com.loopers.application.outbox;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.loopers.application.payment.PaymentResultOutboxService;
import com.loopers.application.payment.PaymentProcessor;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventType;
import com.loopers.domain.outbox.OutboxService;
import com.loopers.domain.payment.PaymentInfo;
import com.loopers.application.platform.DataPlatformApplicationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxDispatcherService {

	private static final int BATCH_SIZE = 100;

	private final OutboxService outboxService;
	private final OutboxPayloadMapper outboxPayloadMapper;
	private final PaymentProcessor paymentProcessor;
	private final PaymentResultOutboxService paymentResultOutboxService;
	private final OrderService orderService;
	private final DataPlatformApplicationService dataPlatformApplicationService;

	public void dispatchPendingEvents() {
		// 스케줄러 1회 실행마다 처리 가능한 PENDING 이벤트를 배치 단위로 가져온다.
		List<Long> pendingEventIds = outboxService.findPendingEventIds(BATCH_SIZE);
		for (Long pendingEventId : pendingEventIds) {
			dispatchSingleEvent(pendingEventId);
		}
	}

	void dispatchSingleEvent(final Long outboxEventId) {
		Optional<OutboxEvent> optionalOutboxEvent = outboxService.claim(outboxEventId);
		if (optionalOutboxEvent.isEmpty()) {
			return;
		}

		OutboxEvent outboxEvent = optionalOutboxEvent.get();

		try {
			dispatchByType(outboxEvent);
			outboxService.complete(outboxEvent.getId());
		} catch (Exception e) {
			log.error("아웃박스 이벤트 처리 실패: id={}, eventType={}, aggregateId={}", outboxEvent.getId(),
				outboxEvent.getEventType(), outboxEvent.getAggregateId(), e);
			outboxService.retry(outboxEvent.getId(), e.getMessage());
		}
	}

	private void dispatchByType(final OutboxEvent outboxEvent) {
		OutboxEventType eventType = outboxEvent.getEventType();
		switch (eventType) {
			// 주문 커밋 이후 실제 결제 요청 실행
			case PAYMENT_REQUEST -> processPaymentRequest(outboxEvent.getPayload());
			// 결제 결과를 주문 상태(CONFIRMED/CANCELLED)로 동기화
			case ORDER_STATUS_SYNC -> processOrderStatusSync(outboxEvent.getPayload());
			// 데이터 플랫폼 전송(실패 시 retry)
			case DATA_PLATFORM_DISPATCH -> processDataPlatformDispatch(outboxEvent.getPayload());
		}
	}

	private void processPaymentRequest(final String payload) {
		OutboxPayload.PaymentRequest requestPayload = outboxPayloadMapper.read(payload, OutboxPayload.PaymentRequest.class);
		PaymentInfo paymentInfo = paymentProcessor.process(requestPayload.toCommand());
		paymentResultOutboxService.enqueueByPaymentStatus(paymentInfo.orderId(), paymentInfo.status());
	}

	private void processOrderStatusSync(final String payload) {
		OutboxPayload.OrderStatusSync orderStatusSync = outboxPayloadMapper.read(payload, OutboxPayload.OrderStatusSync.class);
		orderService.updateOrderStatus(orderStatusSync.orderId(), orderStatusSync.status());
	}

	private void processDataPlatformDispatch(final String payload) {
		OutboxPayload.DataPlatformDispatch dataPlatformDispatch = outboxPayloadMapper.read(payload,
			OutboxPayload.DataPlatformDispatch.class);
		dataPlatformApplicationService.sendEventWithFailure(dataPlatformDispatch.toEvent());
	}
}
