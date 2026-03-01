package com.loopers.domain.payment;

import java.util.List;

public record PaymentInfo(
	String transactionKey,
	String orderId,
	CardType cardType,
	String cardNo,
	Long amount,
	TransactionStatus status,
	String reason
) {

	public static PaymentInfo from(final Payment payment) {
		String transactionKey = payment.getTransactionKey() != null
			? payment.getTransactionKey().getTransactionKey()
			: null;
		String reason = payment.getReason() != null ? payment.getReason().reason() : null;

		return new PaymentInfo(
			transactionKey,
			payment.getOrderId().getOrderId(),
			payment.getCardType(),
			payment.getCardNo().getCardNo(),
			payment.getAmount().getPrice(),
			payment.getStatus(),
			reason
		);
	}

	public record Transaction(
		String transactionKey,
		String orderId,
		Long amount,
		String cardNo,
		CardType cardType,
		TransactionStatus status,
		String reason
	) {
	}

	public record Order(
		String orderId,
		List<TransactionResponse> transactions
	) {
	}

	public record TransactionResponse(
		String transactionKey,
		TransactionStatus status,
		String reason
	) {
	}

}
