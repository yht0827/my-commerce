package com.loopers.domain.outbox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxService {

	private static final int MAX_RETRY_COUNT = 10;
	private static final int RETRY_BASE_SECONDS = 5;
	private static final int RETRY_MAX_SECONDS = 300;

	private final OutboxEventRepository outboxEventRepository;

	@Transactional
	public boolean enqueue(
		final OutboxEventType eventType,
		final String aggregateId,
		final String dedupeKey,
		final String payload
	) {
		return outboxEventRepository.createIfNotExists(eventType, aggregateId, dedupeKey, payload);
	}

	@Transactional(readOnly = true)
	public List<Long> findPendingEventIds(final int limit) {
		return outboxEventRepository.findPendingEventIds(LocalDateTime.now(), limit);
	}

	@Transactional
	public Optional<OutboxEvent> claim(final Long outboxEventId) {
		Optional<OutboxEvent> optionalOutboxEvent = outboxEventRepository.findById(outboxEventId);
		if (optionalOutboxEvent.isEmpty()) {
			return Optional.empty();
		}

		OutboxEvent outboxEvent = optionalOutboxEvent.get();
		if (outboxEvent.getStatus() != OutboxStatus.PENDING) {
			return Optional.empty();
		}
		if (outboxEvent.getNextRetryAt().isAfter(LocalDateTime.now())) {
			return Optional.empty();
		}

		outboxEvent.markProcessing();
		return Optional.of(outboxEvent);
	}

	@Transactional
	public void complete(final Long outboxEventId) {
		outboxEventRepository.findById(outboxEventId)
			.ifPresent(OutboxEvent::markCompleted);
	}

	@Transactional
	public void retry(final Long outboxEventId, final String reason) {
		outboxEventRepository.findById(outboxEventId)
			.ifPresent(outboxEvent -> {
				int nextRetryCount = outboxEvent.getRetryCount() + 1;
				boolean terminalFailure = nextRetryCount >= MAX_RETRY_COUNT;

				long retryDelaySeconds = Math.min(
					RETRY_MAX_SECONDS,
					((long) Math.pow(2, nextRetryCount - 1)) * RETRY_BASE_SECONDS
				);

				outboxEvent.markRetry(reason, LocalDateTime.now().plusSeconds(retryDelaySeconds), terminalFailure);
			});
	}
}
