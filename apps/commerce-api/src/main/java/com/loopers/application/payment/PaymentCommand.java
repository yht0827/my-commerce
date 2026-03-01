package com.loopers.application.payment;

import com.loopers.domain.order.event.OrderCreatedEvent;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.PaymentData;
import com.loopers.domain.payment.TransactionStatus;

public record PaymentCommand() {

	public record CreatePayment(
		String userId,
		String orderId,
		Long amount,
		CardType cardType,
		String cardNo,
		String callbackUrl
	) {
		public static CreatePayment fromOrderEvent(OrderCreatedEvent orderCreatedEvent) {
			return new PaymentCommand.CreatePayment(
				orderCreatedEvent.userId(), orderCreatedEvent.orderId(), orderCreatedEvent.totalAmount(),
				orderCreatedEvent.paymentMetadata().cardType(), orderCreatedEvent.paymentMetadata().cardNo(),
				orderCreatedEvent.paymentMetadata().callbackUrl());
		}

		public PaymentData.PaymentRequest toData() {
			return new PaymentData.PaymentRequest(userId, orderId, cardType, cardNo, amount, callbackUrl);
		}
	}

	public record ProcessCallback(
		String transactionKey,
		TransactionStatus status,
		String orderId,
		String callbackId,
		String callbackTimestamp,
		String callbackSignature,
		String rawBody
	) {
		public PaymentData.ProcessCallback toData() {
			return new PaymentData.ProcessCallback(transactionKey, status, orderId);
		}
	}
}
