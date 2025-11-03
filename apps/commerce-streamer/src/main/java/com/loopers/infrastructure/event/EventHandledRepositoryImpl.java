package com.loopers.infrastructure.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.loopers.domain.event.EventHandled;
import com.loopers.domain.event.EventHandledRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class EventHandledRepositoryImpl implements EventHandledRepository {

	private final EventHandledJpaRepository eventHandledJpaRepository;

	@Override
	public EventHandled save(EventHandled eventHandled) {
		return eventHandledJpaRepository.save(eventHandled);
	}

	@Override
	public void saveAll(List<EventHandled> eventHandledList) {
		eventHandledJpaRepository.saveAll(eventHandledList);
	}

	@Override
	public Optional<EventHandled> findByEventIdAndConsumerGroup(String eventId, String consumerGroup) {
		return eventHandledJpaRepository.findByEventIdAndConsumerGroup(eventId, consumerGroup);
	}

	@Override
	public boolean isNewerVersion(String eventId, String consumerGroup, Long version, LocalDateTime processedAt) {
		Optional<EventHandled> existing = findByEventIdAndConsumerGroup(eventId, consumerGroup);

		if (existing.isEmpty()) {
			return false;
		}

		EventHandled handled = existing.get();

		// 버전 비교
		if (version != null && handled.getVersion() != null) {
			return version <= handled.getVersion();
		}
		// 시간 비교
		if (processedAt != null && handled.getLastProcessedAt() != null) {
			return !processedAt.isAfter(handled.getLastProcessedAt());
		}

		return true;
	}
}
