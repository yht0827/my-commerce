package com.loopers.domain.outbox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository {

	boolean createIfNotExists(OutboxEventType eventType, String aggregateId, String dedupeKey, String payload);

	List<Long> findPendingEventIds(LocalDateTime now, int limit);

	Optional<OutboxEvent> findById(Long id);

	OutboxEvent save(OutboxEvent outboxEvent);
}
