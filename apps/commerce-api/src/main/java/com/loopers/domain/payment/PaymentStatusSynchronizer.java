package com.loopers.domain.payment;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PaymentStatusSynchronizer {

	public void processCallback(Payment payment, PaymentData.ProcessCallback command,
		PaymentInfo.Transaction pgTransaction) {
		if (!isOrderIdMatched(payment, command.orderId())) {
			log.warn("콜백 주문 ID 불일치. transactionKey: {}, callbackOrderId: {}, paymentOrderId: {}",
				command.transactionKey(), command.orderId(), payment.getOrderId().getOrderId());
			return;
		}

		if (isCallbackDataValid(command, pgTransaction)) {
			updatePaymentStatus(payment, command.status());
			log.info("콜백 교차검증 성공. transactionKey: {}, status: {}",
				command.transactionKey(), command.status());
		} else {
			updatePaymentStatus(payment, pgTransaction.status());
			log.warn("콜백 교차검증 실패, PG 실제 상태로 업데이트. transactionKey: {}, callback: {}, pg: {}",
				command.transactionKey(), command.status(), pgTransaction.status());
		}
	}

	public void processCallbackWithException(Payment payment, PaymentData.ProcessCallback command) {
		if (!isOrderIdMatched(payment, command.orderId())) {
			log.warn("콜백 주문 ID 불일치(예외 처리 경로). transactionKey: {}, callbackOrderId: {}, paymentOrderId: {}",
				command.transactionKey(), command.orderId(), payment.getOrderId().getOrderId());
			return;
		}

		log.error("콜백 교차검증 실패, 콜백 데이터로 처리. transactionKey: {}", command.transactionKey());
		updatePaymentStatus(payment, command.status());
	}

	public void processScheduler(Payment payment, TransactionStatus pgStatus) {
		if (pgStatus != TransactionStatus.PENDING && payment.getStatus() == TransactionStatus.PENDING) {
			updatePaymentStatus(payment, pgStatus);
			log.info("스케줄러 - 결제 상태 동기화: PENDING → {}, transactionKey: {}",
				pgStatus, payment.getTransactionKey().getTransactionKey());
		}
	}

	private boolean isCallbackDataValid(PaymentData.ProcessCallback command, PaymentInfo.Transaction pgTransaction) {
		if (pgTransaction == null || pgTransaction.status() == null) {
			return false;
		}

		if (!command.transactionKey().equals(pgTransaction.transactionKey())) {
			return false;
		}

		if (!isOrderIdMatched(command.orderId(), pgTransaction.orderId())) {
			return false;
		}

		return command.status() == pgTransaction.status();
	}

	private boolean isOrderIdMatched(Payment payment, String callbackOrderId) {
		return isOrderIdMatched(callbackOrderId, payment.getOrderId().getOrderId());
	}

	private boolean isOrderIdMatched(String callbackOrderId, String targetOrderId) {
		if (callbackOrderId == null || callbackOrderId.isBlank()) {
			return true;
		}
		if (targetOrderId == null || targetOrderId.isBlank()) {
			return false;
		}
		return callbackOrderId.equals(targetOrderId);
	}

	private void updatePaymentStatus(Payment payment, TransactionStatus status) {
		if (status == null) {
			log.warn("결제 상태가 null 이므로 상태 변경을 건너뜁니다. transactionKey: {}",
				payment.getTransactionKey() != null ? payment.getTransactionKey().getTransactionKey() : "N/A");
			return;
		}

		if (payment.getStatus() == status) {
			log.info("중복 상태 콜백 감지. transactionKey: {}, status: {}",
				payment.getTransactionKey() != null ? payment.getTransactionKey().getTransactionKey() : "N/A", status);
			return;
		}

		if (payment.getStatus() != TransactionStatus.PENDING) {
			log.warn("터미널 상태 결제의 상태 변경 요청을 무시합니다. transactionKey: {}, current: {}, requested: {}",
				payment.getTransactionKey() != null ? payment.getTransactionKey().getTransactionKey() : "N/A",
				payment.getStatus(),
				status);
			return;
		}

		switch (status) {
			case SUCCESS -> payment.processPaymentSuccess();
			case FAILED -> payment.processPaymentFailed();
			case INITIAL, PENDING -> payment.processPaymentPending();
		}
	}
}
