package com.loopers.domain.event;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.loopers.config.kafka.KafkaGroups;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogService {

	private final EventLogRepository eventLogRepository;
	private final EventHandledRepository eventHandledRepository;

	public void processAuditLogs(List<AuditLogData> auditLogDataList) {
		List<EventLog> eventLogs = new ArrayList<>();
		List<EventHandled> handledEvents = new ArrayList<>();

		for (AuditLogData data : auditLogDataList) {
			try {
				// 중복/버전 체크
				if (eventHandledRepository.isNewerVersion(data.eventId(), KafkaGroups.AuditLog,
					data.version(), data.occurredAt())) {
					log.debug("이미 처리된 또는 오래된 감사 로그 이벤트 - EventId: {}, Version: {}, OccurredAt: {}",
						data.eventId(), data.version(), data.occurredAt());
					continue;
				}


				EventLog eventLog = createEventLog(data);
				EventHandled eventHandled = createEventHandled(data);


				// 처리된 이벤트 기록
				eventLogs.add(eventLog);
				handledEvents.add(eventHandled);

			} catch (Exception e) {
				log.error("감사 로그 처리 실패 - EventId: {}, Topic: {}", 
					data.eventId(), data.topic(), e);
			}
		}

		// 배치 저장
		saveAuditLogs(eventLogs, handledEvents);
	}

	public EventLog createEventLog(AuditLogData data) {
		return EventLog.builder()
			.eventId(data.eventId())
			.eventType(data.eventType())
			.topic(data.topic())
			.partitionId(data.partition())
			.offset(data.offset())
			.payload(data.payload())
			.occurredAt(data.occurredAt())
			.build();
	}

	public EventHandled createEventHandled(AuditLogData data) {
		return EventHandled.builder()
			.eventId(data.eventId())
			.consumerGroup(KafkaGroups.AuditLog)
			.version(data.version())
			.lastProcessedAt(LocalDateTime.now())
			.build();
	}

	public void saveAuditLogs(List<EventLog> eventLogs, List<EventHandled> handledEvents) {
		if (!eventLogs.isEmpty()) {
			eventLogRepository.saveAll(eventLogs);
			eventHandledRepository.saveAll(handledEvents);
			log.info("감사 로그 배치 저장 완료 - 총 {}건 처리", eventLogs.size());
		} else {
			log.debug("처리할 새로운 감사 로그가 없습니다");
		}
	}
}
