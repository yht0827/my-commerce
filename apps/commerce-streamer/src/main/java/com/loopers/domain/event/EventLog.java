package com.loopers.domain.event;

import java.time.LocalDateTime;

import com.loopers.domain.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "event_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventLog extends BaseEntity {

	private String eventId;
	private String eventType;
	private String topic;
	private Integer partitionId;
	private Long offset;
	private String payload;
	private LocalDateTime occurredAt;

	@Builder
	public EventLog(String eventId, String eventType, String topic, Integer partitionId, Long offset, String payload,
		LocalDateTime occurredAt) {
		this.eventId = eventId;
		this.eventType = eventType;
		this.topic = topic;
		this.partitionId = partitionId;
		this.offset = offset;
		this.payload = payload;
		this.occurredAt = occurredAt;
	}
}
