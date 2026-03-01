package com.loopers.domain.payment;

import org.springframework.stereotype.Service;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final PaymentGatewayService paymentGatewayService;

	public Payment createPayment(final PaymentData.PaymentRequest data) {
		Payment payment = data.toEntity();
		return paymentRepository.save(payment);
	}

	public PaymentInfo updatePaymentStatus(Payment payment, PaymentInfo.Transaction pgResponse) {
		if (pgResponse.transactionKey() != null && !pgResponse.transactionKey().isBlank()) {
			payment.updateTransactionKey(pgResponse.transactionKey());
		}

		TransactionStatus status = pgResponse.status() != null ? pgResponse.status() : TransactionStatus.PENDING;

		switch (status) {
			case SUCCESS -> payment.processPaymentSuccess();
			case FAILED -> payment.processPaymentFailed();
			case INITIAL, PENDING -> payment.processPaymentPending();
		}

		return PaymentInfo.from(payment);
	}

	public PaymentInfo processCallback(PaymentData.ProcessCallback command) {
		Payment payment = paymentRepository.findByTransactionKeyForUpdate(command.transactionKey())
			.orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));

		return paymentGatewayService.processCallbackWithVerification(payment, command);
	}
}
