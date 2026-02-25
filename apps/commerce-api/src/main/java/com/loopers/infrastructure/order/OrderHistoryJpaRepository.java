package com.loopers.infrastructure.order;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loopers.domain.order.OrderHistory;
import com.loopers.domain.user.UserId;

public interface OrderHistoryJpaRepository extends JpaRepository<OrderHistory, Long> {

	@Modifying
	@Query(
		value = "INSERT IGNORE INTO order_history (user_id, idempotency_key, created_at, updated_at, deleted_at) "
			+ "VALUES (:userId, :idempotencyKey, NOW(6), NOW(6), NULL)",
		nativeQuery = true
	)
	int createIfNotExists(@Param("userId") String userId, @Param("idempotencyKey") String idempotencyKey);

	Optional<OrderHistory> findByUserIdAndIdempotencyKey(UserId userId, String idempotencyKey);
}
