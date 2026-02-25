package com.loopers.application.payment;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.domain.order.event.OrderCreatedEvent;
import com.loopers.domain.payment.TransactionStatus;
import com.loopers.domain.payment.event.PaymentCompletedEvent;
import com.loopers.domain.payment.event.PaymentFailedEvent;
import com.loopers.domain.platform.event.DataPlatformEvent;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.support.event.Envelope;
import com.loopers.support.event.EventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventHandler {

	private final PaymentApplicationService paymentApplicationService;
	private final EventPublisher eventPublisher;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void processOrderCreated(Envelope<OrderCreatedEvent> event) {
		if (!OrderCreatedEvent.EVENT_TYPE.equals(event.getEventType())) {
			return;
		}

		OrderCreatedEvent orderEvent = event.getPayload();
		PaymentResult result = paymentApplicationService.processOrderPayment(orderEvent);

		handlePaymentResult(result);
	}

	void handlePaymentResult(PaymentResult result) {
		switch (result) {
			case PaymentResult.Success success -> handleSuccessByStatus(success);
			case PaymentResult.Failure failure -> handleFailure(failure);
			default -> throw new CoreException(ErrorType.NOT_FOUND, "예상하지 못한 오류가 발생 하였습니다.");
		}
	}

	void handleSuccessByStatus(PaymentResult.Success success) {
		TransactionStatus status = success.paymentInfo().status();
		if (status == TransactionStatus.SUCCESS) {
			handleSuccess(success);
			return;
		}

		log.info("결제 요청 처리 결과가 터미널 성공 상태가 아니므로 완료 이벤트 발행을 건너뜁니다. orderId: {}, status: {}",
			success.paymentInfo().orderId(), status);
	}

	void handleSuccess(PaymentResult.Success success) {
		PaymentCompletedEvent completedEvent = PaymentCompletedEvent.create(success.paymentInfo().orderId(),
			success.paymentInfo().transactionKey());
		eventPublisher.publish(completedEvent);
		log.info("결제 처리 성공 - 주문ID: {}, 결제ID: {}", success.paymentInfo().orderId(), success.paymentInfo().transactionKey());

		DataPlatformEvent dataPlatformEvent = DataPlatformEvent.create(completedEvent.getEventType(), completedEvent.orderId());
		eventPublisher.publish(dataPlatformEvent);
		log.info("결제 성공 이벤트를 데이터 플랫폼 이벤트로 발행: {}", success.paymentInfo().orderId());
	}

	void handleFailure(PaymentResult.Failure failure) {
		PaymentFailedEvent failedEvent = PaymentFailedEvent.create(failure.orderEvent().orderId(), failure.orderEvent().userId(),
			"FAILED_" + failure.orderEvent().orderId(), failure.orderEvent().totalAmount(), failure.exception());
		eventPublisher.publish(failedEvent);
		log.error("결제 처리 실패 - 주문ID: {}", failure.orderEvent().orderId(), failure.exception());

		DataPlatformEvent dataPlatformEvent = DataPlatformEvent.create(failedEvent.getEventType(), failedEvent.orderId());
		eventPublisher.publish(dataPlatformEvent);
		log.info("결제 실패 이벤트를 데이터 플랫폼 이벤트로 발행: {}", failure.orderEvent().orderId());
	}
}
