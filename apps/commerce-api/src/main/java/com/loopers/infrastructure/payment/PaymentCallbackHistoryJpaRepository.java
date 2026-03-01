package com.loopers.infrastructure.payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loopers.domain.payment.PaymentCallbackHistory;

public interface PaymentCallbackHistoryJpaRepository extends JpaRepository<PaymentCallbackHistory, Long> {

	@Modifying
	@Query(
		value = "INSERT IGNORE INTO payment_callback_history "
			+ "(dedupe_key, transaction_key, order_id, callback_status, process_status, created_at, updated_at, deleted_at) "
			+ "VALUES (:dedupeKey, :transactionKey, :orderId, :callbackStatus, 'RECEIVED', NOW(6), NOW(6), NULL)",
		nativeQuery = true
	)
	int createIfNotExists(
		@Param("dedupeKey") String dedupeKey,
		@Param("transactionKey") String transactionKey,
		@Param("orderId") String orderId,
		@Param("callbackStatus") String callbackStatus
	);

	Optional<PaymentCallbackHistory> findByDedupeKey(String dedupeKey);
}
