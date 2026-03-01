package com.loopers.domain.payment;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {

	Payment save(final Payment payment);

	Optional<Payment> findById(final Long id);

	Optional<Payment> findByTransactionKey(final String transactionKey);

	Optional<Payment> findByTransactionKeyForUpdate(final String transactionKey);

	List<Payment> findByStatus(final TransactionStatus status);
}
