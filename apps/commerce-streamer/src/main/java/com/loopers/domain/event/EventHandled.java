package com.loopers.domain.event;

import java.time.LocalDateTime;

import com.loopers.domain.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Entity
@Table(name = "event_handled")
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class EventHandled extends BaseEntity {

	private String eventId;

	private String consumerGroup;

	private Long version;

	private LocalDateTime lastProcessedAt;

	@Builder
	public EventHandled(String eventId, String consumerGroup, Long version, LocalDateTime lastProcessedAt) {
		this.eventId = eventId;
		this.consumerGroup = consumerGroup;
		this.version = version;
		this.lastProcessedAt = lastProcessedAt;
	}

}
