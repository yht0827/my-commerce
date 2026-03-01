package com.loopers.interfaces.scheduler.payment;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.loopers.application.payment.PaymentResultOutboxService;
import com.loopers.domain.payment.PaymentGatewayService;
import com.loopers.domain.payment.PaymentInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentVerificationScheduler {

	private final PaymentGatewayService paymentGatewayService;
	private final PaymentResultOutboxService paymentResultOutboxService;

	@Scheduled(fixedDelay = 60000)
	public void verifyPendingPayments() {
		// PG 재검증으로 terminal 상태가 된 결제만 주문/플랫폼 동기화 outbox를 추가한다.
		for (PaymentInfo paymentInfo : paymentGatewayService.verifyPendingPayments()) {
			paymentResultOutboxService.enqueueByPaymentStatus(paymentInfo.orderId(), paymentInfo.status());
		}
	}
}
