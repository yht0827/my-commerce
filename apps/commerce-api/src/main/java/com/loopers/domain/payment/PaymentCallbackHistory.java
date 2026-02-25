package com.loopers.domain.payment;

import static com.loopers.support.error.ErrorType.*;

import java.time.ZonedDateTime;

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
	name = "payment_callback_history",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_payment_callback_history_dedupe_key", columnNames = {"dedupe_key"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentCallbackHistory extends BaseEntity {

	private static final int MAX_DEDUPE_KEY_LENGTH = 128;

	@Column(name = "dedupe_key", nullable = false, length = MAX_DEDUPE_KEY_LENGTH)
	private String dedupeKey;

	@Column(name = "transaction_key", length = 255)
	private String transactionKey;

	@Column(name = "order_id", length = 50)
	private String orderId;

	@Enumerated(EnumType.STRING)
	@Column(name = "callback_status", length = 20)
	private TransactionStatus callbackStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "process_status", nullable = false, length = 20)
	private PaymentCallbackProcessStatus processStatus;

	@Column(name = "failed_reason", length = 255)
	private String failedReason;

	@Column(name = "processed_at")
	private ZonedDateTime processedAt;

	@Builder(builderMethodName = "create")
	public PaymentCallbackHistory(
		final String dedupeKey,
		final String transactionKey,
		final String orderId,
		final TransactionStatus callbackStatus,
		final PaymentCallbackProcessStatus processStatus,
		final String failedReason,
		final ZonedDateTime processedAt
	) {
		this.dedupeKey = normalize(dedupeKey);
		this.transactionKey = transactionKey;
		this.orderId = orderId;
		this.callbackStatus = callbackStatus;
		this.processStatus = processStatus;
		this.failedReason = failedReason;
		this.processedAt = processedAt;
	}

	@Override
	protected void guard() {
		if (dedupeKey == null || dedupeKey.isBlank()) {
			throw new CoreException(BAD_REQUEST, "콜백 멱등 키는 비어있을 수 없습니다.");
		}
		if (dedupeKey.length() > MAX_DEDUPE_KEY_LENGTH) {
			throw new CoreException(BAD_REQUEST, "콜백 멱등 키 길이가 너무 깁니다.");
		}
		if (processStatus == null) {
			throw new CoreException(BAD_REQUEST, "콜백 처리 상태는 필수입니다.");
		}
	}

	public void markProcessing() {
		if (processStatus == PaymentCallbackProcessStatus.COMPLETED) {
			return;
		}
		this.processStatus = PaymentCallbackProcessStatus.PROCESSING;
	}

	public void complete() {
		this.processStatus = PaymentCallbackProcessStatus.COMPLETED;
		this.failedReason = null;
		this.processedAt = ZonedDateTime.now();
	}

	public void fail(final String reason) {
		this.processStatus = PaymentCallbackProcessStatus.FAILED;
		this.failedReason = reason;
		this.processedAt = ZonedDateTime.now();
	}

	private static String normalize(final String key) {
		if (key == null) {
			return null;
		}
		return key.trim();
	}
}
