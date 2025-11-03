package com.loopers.application;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.event.AuditLogData;
import com.loopers.domain.event.AuditLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogFacade {

	private final AuditLogService auditLogService;
	private final AuditLogProcessor auditLogProcessor;

	@Transactional
	public void handleAuditLog(List<ConsumerRecord<String, Object>> records) {
		try {

			// Kafka 레코드 데이터 처리
			List<AuditLogData> auditLogData = auditLogProcessor.parseRecords(records);

			auditLogService.processAuditLogs(auditLogData);

			log.info("감사 로그 레코드 파싱 완료 - 수신: {}건, 파싱 성공: {}건", records.size(), auditLogData.size());
		} catch (Exception e) {
			log.error("감사 로그 처리 중 오류 발생", e);
			throw e;
		}
	}

}
