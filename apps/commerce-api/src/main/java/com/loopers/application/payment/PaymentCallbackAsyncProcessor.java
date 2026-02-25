package com.loopers.application.payment;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.payment.PaymentInfo;
import com.loopers.domain.payment.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentCallbackAsyncProcessor {

	private final PaymentService paymentService;
	private final PaymentCallbackHistoryService paymentCallbackHistoryService;
	private final PaymentResultOutboxService paymentResultOutboxService;

	@Async
	@Transactional
	public void process(final String dedupeKey, final PaymentCommand.ProcessCallback command) {
		try {
			paymentCallbackHistoryService.markProcessing(dedupeKey);
			PaymentInfo paymentInfo = paymentService.processCallback(command.toData());
			paymentResultOutboxService.enqueueByPaymentStatus(paymentInfo.orderId(), paymentInfo.status());
			paymentCallbackHistoryService.complete(dedupeKey);
			log.info("결제 콜백 비동기 처리 완료. dedupeKey: {}, transactionKey: {}", dedupeKey, command.transactionKey());
		} catch (Exception e) {
			try {
				paymentCallbackHistoryService.fail(dedupeKey, e.getMessage());
			} catch (Exception historyException) {
				log.error("결제 콜백 이력 실패 상태 업데이트에 실패했습니다. dedupeKey: {}", dedupeKey, historyException);
			}
			log.error("결제 콜백 비동기 처리 실패. dedupeKey: {}, transactionKey: {}", dedupeKey, command.transactionKey(), e);
		}
	}
}
