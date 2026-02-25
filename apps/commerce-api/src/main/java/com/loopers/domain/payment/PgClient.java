package com.loopers.domain.payment;

public interface PgClient {

	PaymentInfo.Transaction request(PaymentData.PaymentRequest request);

	PaymentInfo.Order findOrder(final String orderId);

	PaymentInfo.Transaction findTransaction(final String transactionKey);
}
