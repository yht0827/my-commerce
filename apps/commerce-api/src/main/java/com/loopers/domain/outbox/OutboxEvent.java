package com.loopers.domain.outbox;

import static com.loopers.support.error.ErrorType.*;

import java.time.LocalDateTime;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
	name = "event_outbox",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_event_outbox_dedupe_key", columnNames = {"dedupe_key"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends BaseEntity {

	private static final int MAX_EVENT_TYPE_LENGTH = 50;
	private static final int MAX_AGGREGATE_ID_LENGTH = 100;
	private static final int MAX_DEDUPE_KEY_LENGTH = 150;
	private static final int MAX_LAST_ERROR_LENGTH = 500;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = MAX_EVENT_TYPE_LENGTH)
	private OutboxEventType eventType;

	@Column(name = "aggregate_id", nullable = false, length = MAX_AGGREGATE_ID_LENGTH)
	private String aggregateId;

	@Column(name = "dedupe_key", nullable = false, length = MAX_DEDUPE_KEY_LENGTH)
	private String dedupeKey;

	@Column(name = "payload", nullable = false, columnDefinition = "LONGTEXT")
	private String payload;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private OutboxStatus status;

	@Column(name = "retry_count", nullable = false)
	private int retryCount;

	@Column(name = "next_retry_at", nullable = false)
	private LocalDateTime nextRetryAt;

	@Column(name = "last_error", length = MAX_LAST_ERROR_LENGTH)
	private String lastError;

	@Builder(builderMethodName = "create")
	public OutboxEvent(
		final OutboxEventType eventType,
		final String aggregateId,
		final String dedupeKey,
		final String payload,
		final OutboxStatus status,
		final int retryCount,
		final LocalDateTime nextRetryAt,
		final String lastError
	) {
		this.eventType = eventType;
		this.aggregateId = normalize(aggregateId);
		this.dedupeKey = normalize(dedupeKey);
		this.payload = payload;
		this.status = status;
		this.retryCount = retryCount;
		this.nextRetryAt = nextRetryAt;
		this.lastError = trimToLimit(lastError, MAX_LAST_ERROR_LENGTH);
	}

	@Override
	protected void guard() {
		if (eventType == null) {
			throw new CoreException(BAD_REQUEST, "아웃박스 이벤트 타입은 필수입니다.");
		}
		if (aggregateId == null || aggregateId.isBlank()) {
			throw new CoreException(BAD_REQUEST, "아웃박스 aggregateId는 비어있을 수 없습니다.");
		}
		if (dedupeKey == null || dedupeKey.isBlank()) {
			throw new CoreException(BAD_REQUEST, "아웃박스 dedupeKey는 비어있을 수 없습니다.");
		}
		if (payload == null || payload.isBlank()) {
			throw new CoreException(BAD_REQUEST, "아웃박스 payload는 비어있을 수 없습니다.");
		}
		if (status == null) {
			throw new CoreException(BAD_REQUEST, "아웃박스 상태는 필수입니다.");
		}
		if (retryCount < 0) {
			throw new CoreException(BAD_REQUEST, "아웃박스 재시도 횟수는 음수일 수 없습니다.");
		}
		if (nextRetryAt == null) {
			throw new CoreException(BAD_REQUEST, "아웃박스 nextRetryAt은 필수입니다.");
		}
	}

	public void markProcessing() {
		if (status == OutboxStatus.COMPLETED || status == OutboxStatus.FAILED) {
			return;
		}
		this.status = OutboxStatus.PROCESSING;
		this.lastError = null;
	}

	public void markCompleted() {
		this.status = OutboxStatus.COMPLETED;
		this.lastError = null;
	}

	public void markRetry(final String reason, final LocalDateTime retryAt, final boolean terminalFailure) {
		this.retryCount += 1;
		this.lastError = trimToLimit(reason, MAX_LAST_ERROR_LENGTH);
		if (terminalFailure) {
			this.status = OutboxStatus.FAILED;
			this.nextRetryAt = LocalDateTime.now();
			return;
		}

		this.status = OutboxStatus.PENDING;
		this.nextRetryAt = retryAt;
	}

	private static String normalize(final String value) {
		if (value == null) {
			return null;
		}
		return value.trim();
	}

	private static String trimToLimit(final String value, final int maxLength) {
		if (value == null) {
			return null;
		}
		if (value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}
}
