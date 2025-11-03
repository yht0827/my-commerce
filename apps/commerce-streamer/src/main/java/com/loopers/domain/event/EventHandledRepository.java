package com.loopers.domain.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventHandledRepository {

	EventHandled save(EventHandled eventHandled);

	void saveAll(List<EventHandled> eventHandledList);

	Optional<EventHandled> findByEventIdAndConsumerGroup(String eventId, String consumerGroup);

	boolean isNewerVersion(String eventId, String consumerGroup, Long version, LocalDateTime processedAt);
}
