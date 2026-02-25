package com.loopers.infrastructure.outbox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import com.loopers.domain.outbox.OutboxEventType;
import com.loopers.domain.outbox.OutboxStatus;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

	private final OutboxEventJpaRepository outboxEventJpaRepository;

	@Override
	public boolean createIfNotExists(
		final OutboxEventType eventType,
		final String aggregateId,
		final String dedupeKey,
		final String payload
	) {
		return outboxEventJpaRepository.createIfNotExists(eventType.name(), aggregateId, dedupeKey, payload) > 0;
	}

	@Override
	public List<Long> findPendingEventIds(final LocalDateTime now, final int limit) {
		return outboxEventJpaRepository.findPendingIds(OutboxStatus.PENDING, now, PageRequest.of(0, limit));
	}

	@Override
	public Optional<OutboxEvent> findById(final Long id) {
		return outboxEventJpaRepository.findById(id);
	}

	@Override
	public OutboxEvent save(final OutboxEvent outboxEvent) {
		return outboxEventJpaRepository.save(outboxEvent);
	}
}
