package com.loopers.infrastructure.payment;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.loopers.domain.payment.PaymentCallbackHistory;
import com.loopers.domain.payment.PaymentCallbackHistoryRepository;
import com.loopers.domain.payment.TransactionStatus;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PaymentCallbackHistoryRepositoryImpl implements PaymentCallbackHistoryRepository {

	private final PaymentCallbackHistoryJpaRepository paymentCallbackHistoryJpaRepository;

	@Override
	public boolean createIfNotExists(
		final String dedupeKey,
		final String transactionKey,
		final String orderId,
		final TransactionStatus callbackStatus
	) {
		String status = callbackStatus != null ? callbackStatus.name() : null;
		return paymentCallbackHistoryJpaRepository.createIfNotExists(dedupeKey, transactionKey, orderId, status) > 0;
	}

	@Override
	public Optional<PaymentCallbackHistory> findByDedupeKey(final String dedupeKey) {
		return paymentCallbackHistoryJpaRepository.findByDedupeKey(dedupeKey);
	}
}
