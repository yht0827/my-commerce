package com.loopers.application.payment;

import org.springframework.stereotype.Service;

import com.loopers.domain.order.event.OrderCreatedEvent;
import com.loopers.domain.payment.PaymentInfo;
import com.loopers.domain.payment.PaymentService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentApplicationService {

	private final PaymentService paymentService;
	private final PaymentProcessor paymentProcessor;
	private final PaymentCallbackSignatureVerifier paymentCallbackSignatureVerifier;
	private final PaymentCallbackHistoryService paymentCallbackHistoryService;
	private final PaymentCallbackAsyncProcessor paymentCallbackAsyncProcessor;

	public boolean acceptCallback(final PaymentCommand.ProcessCallback command) {
		paymentCallbackSignatureVerifier.verify(command);

		String dedupeKey = paymentCallbackHistoryService.generateDedupeKey(command);
		if (!paymentCallbackHistoryService.claim(dedupeKey, command)) {
			return false;
		}

		paymentCallbackAsyncProcessor.process(dedupeKey, command);
		return true;
	}

	@Transactional
	public PaymentResult processCallback(final PaymentCommand.ProcessCallback command) {
		PaymentInfo paymentInfo = paymentService.processCallback(command.toData());
		return PaymentResult.success(paymentInfo);
	}

	public PaymentResult processOrderPayment(final OrderCreatedEvent orderEvent) {
		try {
			if (orderEvent.paymentMetadata() == null) {
				throw new CoreException(ErrorType.NOT_FOUND, "결제 정보 없음");
			}

			PaymentCommand.CreatePayment command = PaymentCommand.CreatePayment.fromOrderEvent(orderEvent);
			PaymentInfo paymentInfo = paymentProcessor.process(command);

			return PaymentResult.success(paymentInfo);
		} catch (Exception e) {
			return PaymentResult.failure(orderEvent, e);
		}
	}
}
