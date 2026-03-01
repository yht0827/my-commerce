package com.loopers.infrastructure.payment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.TransactionStatus;

import jakarta.persistence.LockModeType;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {

	@Query("SELECT p FROM Payment p WHERE p.transactionKey.transactionKey = :transactionKey")
	Optional<Payment> findByTransactionKeyTransactionKey(String transactionKey);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM Payment p WHERE p.transactionKey.transactionKey = :transactionKey")
	Optional<Payment> findByTransactionKeyTransactionKeyForUpdate(String transactionKey);

	@Query("SELECT p FROM Payment p WHERE p.status = :status")
	List<Payment> findByStatus(TransactionStatus status);

}
