package com.loopers.domain.payment;

import java.util.Optional;

public interface PaymentCallbackHistoryRepository {

	boolean createIfNotExists(String dedupeKey, String transactionKey, String orderId, TransactionStatus callbackStatus);

	Optional<PaymentCallbackHistory> findByDedupeKey(String dedupeKey);
}
