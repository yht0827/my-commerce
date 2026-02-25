package com.loopers.application.payment;

import org.springframework.stereotype.Component;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentData;
import com.loopers.domain.payment.PaymentGatewayService;
import com.loopers.domain.payment.PaymentInfo;
import com.loopers.domain.payment.PaymentService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentProcessor {
	private final PaymentService paymentService;
	private final PaymentGatewayService paymentGatewayService;

	public PaymentInfo process(PaymentCommand.CreatePayment command) {
		PaymentData.PaymentRequest data = command.toData();

		Payment payment = paymentService.createPayment(data);

		PaymentInfo.Transaction pgResponse = paymentGatewayService.requestPaymentGateway(data);

		return paymentService.updatePaymentStatus(payment, pgResponse);
	}
}
