package com.loopers.application.outbox;

import com.loopers.application.payment.PaymentCommand;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.platform.event.DataPlatformEvent;

public record OutboxPayload() {

	public record PaymentRequest(
		String userId,
		String orderId,
		Long amount,
		CardType cardType,
		String cardNo,
		String callbackUrl
	) {
		public PaymentCommand.CreatePayment toCommand() {
			return new PaymentCommand.CreatePayment(userId, orderId, amount, cardType, cardNo, callbackUrl);
		}
	}

	public record OrderStatusSync(String orderId, OrderStatus status) {
	}

	public record DataPlatformDispatch(String dataType, String aggregateId) {
		public DataPlatformEvent toEvent() {
			return DataPlatformEvent.create(dataType, aggregateId);
		}
	}
}
