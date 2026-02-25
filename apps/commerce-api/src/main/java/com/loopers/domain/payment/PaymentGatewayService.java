package com.loopers.domain.payment;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentGatewayService {
	private final PgClient pgClient;
	private final PaymentRepository paymentRepository;
	private final PaymentStatusSynchronizer paymentStatusSynchronizer;

	public PaymentInfo.Transaction requestPaymentGateway(final PaymentData.PaymentRequest data) {
		return pgClient.request(data);
	}

	@Transactional
	public List<PaymentInfo> verifyPendingPayments() {
		List<Payment> pendingPayments = paymentRepository.findByStatus(TransactionStatus.PENDING);
		List<PaymentInfo> terminalTransitionPayments = new ArrayList<>();
		log.info("결제 상태 확인 시작. 대상 건수: {}", pendingPayments.size());

		for (Payment payment : pendingPayments) {
			try {
				TransactionStatus beforeStatus = payment.getStatus();
				verifyAndSyncPaymentStatusByScheduler(payment);
				TransactionStatus afterStatus = payment.getStatus();
				if (beforeStatus != afterStatus
					&& (afterStatus == TransactionStatus.SUCCESS || afterStatus == TransactionStatus.FAILED)) {
					terminalTransitionPayments.add(PaymentInfo.from(payment));
				}
			} catch (Exception e) {
				log.error("결제 상태 확인 실패. transactionKey: {}, error: {}",
					transactionKeyOf(payment), e.getMessage());
			}
		}

		log.info("결제 상태 확인 완료. 대상 건수: {}", pendingPayments.size());
		return terminalTransitionPayments;
	}

	private void verifyAndSyncPaymentStatusByScheduler(Payment payment) {
		PaymentInfo.Order pgOrder = pgClient.findOrder(payment.getOrderId().getOrderId());

		// PG 상태에 따라 동기화
		if (pgOrder == null || pgOrder.transactions() == null || pgOrder.transactions().isEmpty()) {
			log.warn("PG 응답이 비어있음. transactionKey: {}", transactionKeyOf(payment));
			return;
		}

		String transactionKey = payment.getTransactionKey() != null ? payment.getTransactionKey().getTransactionKey() : null;
		PaymentInfo.TransactionResponse transaction = pgOrder.transactions().stream()
			.filter(response -> transactionKey != null && transactionKey.equals(response.transactionKey()))
			.findFirst()
			.orElse(pgOrder.transactions().getFirst());

		TransactionStatus pgStatus = transaction.status();
		paymentStatusSynchronizer.processScheduler(payment, pgStatus);
	}

	public PaymentInfo processCallbackWithVerification(Payment payment, PaymentData.ProcessCallback command) {
		try {
			PaymentInfo.Transaction pgTransaction = pgClient.findTransaction(command.transactionKey());
			paymentStatusSynchronizer.processCallback(payment, command, pgTransaction);
		} catch (PgClientException e) {
			log.warn("PG 조회 실패, 콜백 데이터로 처리. transactionKey: {}, error: {}",
				command.transactionKey(), e.getMessage());
			paymentStatusSynchronizer.processCallbackWithException(payment, command);
		}

		return PaymentInfo.from(payment);
	}

	private String transactionKeyOf(final Payment payment) {
		return payment.getTransactionKey() != null ? payment.getTransactionKey().getTransactionKey() : "N/A";
	}
}
