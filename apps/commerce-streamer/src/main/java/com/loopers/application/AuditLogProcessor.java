package com.loopers.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.event.AuditLogData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogProcessor {

	private final ObjectMapper objectMapper;

	public List<AuditLogData> parseRecords(List<ConsumerRecord<String, Object>> records) {
		List<AuditLogData> auditLogDataList = new ArrayList<>();

		for (ConsumerRecord<String, Object> record : records) {
			try {
				// 이벤트 데이터 파싱
				String eventId = extractEventId(record.value());
				Long version = extractVersion(record.value());
				LocalDateTime occurredAt = extractOccurredAt(record.value());
				String eventType = determineEventType(record.value());
				String payload = objectMapper.writeValueAsString(record.value());

				AuditLogData auditLogData = AuditLogData.of(
					eventId,
					eventType,
					record.topic(),
					record.partition(),
					record.offset(),
					payload,
					occurredAt,
					version
				);

				auditLogDataList.add(auditLogData);

			} catch (Exception e) {
				log.error("레코드 파싱 실패 - Topic: {}, Partition: {}, Offset: {}",
					record.topic(), record.partition(), record.offset(), e);
			}
		}

		return auditLogDataList;
	}

	public String extractEventId(Object payload) {
		try {
			JsonNode node = objectMapper.valueToTree(payload);
			return node.has("eventId") ? node.get("eventId").asText() :
				node.has("id") ? node.get("id").asText() :
					"unknown-" + System.currentTimeMillis();
		} catch (Exception e) {
			log.warn("이벤트 ID 추출 실패", e);
			return "unknown-" + System.currentTimeMillis();
		}
	}

	public Long extractVersion(Object payload) {
		try {
			JsonNode node = objectMapper.valueToTree(payload);
			if (node.has("version")) {
				return node.get("version").asLong();
			}
			if (node.has("eventVersion")) {
				return node.get("eventVersion").asLong();
			}
		} catch (Exception e) {
			log.debug("버전 추출 실패", e);
		}
		return null;
	}

	public String determineEventType(Object payload) {
		try {
			JsonNode node = objectMapper.valueToTree(payload);
			return node.has("eventType") ? node.get("eventType").asText() :
				payload.getClass().getSimpleName();
		} catch (Exception e) {
			return "UNKNOWN";
		}
	}

	private LocalDateTime extractOccurredAt(Object payload) {
		try {
			JsonNode node = objectMapper.valueToTree(payload);
			if (node.has("occurredAt")) {
				return LocalDateTime.parse(node.get("occurredAt").asText());
			}
			if (node.has("createdAt")) {
				return LocalDateTime.parse(node.get("createdAt").asText());
			}
		} catch (Exception e) {
			log.debug("발생 시간 추출 실패", e);
		}
		return LocalDateTime.now();
	}
}
