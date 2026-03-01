package com.loopers.infrastructure.outbox;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxStatus;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, Long> {

	@Modifying
	@Query(
		value = "INSERT IGNORE INTO event_outbox "
			+ "(event_type, aggregate_id, dedupe_key, payload, status, retry_count, next_retry_at, created_at, updated_at, deleted_at) "
			+ "VALUES (:eventType, :aggregateId, :dedupeKey, :payload, 'PENDING', 0, NOW(6), NOW(6), NOW(6), NULL)",
		nativeQuery = true
	)
	int createIfNotExists(
		@Param("eventType") String eventType,
		@Param("aggregateId") String aggregateId,
		@Param("dedupeKey") String dedupeKey,
		@Param("payload") String payload
	);

	@Query("SELECT o.id FROM OutboxEvent o WHERE o.status = :status AND o.nextRetryAt <= :now ORDER BY o.id ASC")
	List<Long> findPendingIds(
		@Param("status") OutboxStatus status,
		@Param("now") LocalDateTime now,
		Pageable pageable
	);
}
