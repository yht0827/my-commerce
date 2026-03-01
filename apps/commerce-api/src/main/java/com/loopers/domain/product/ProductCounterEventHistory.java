package com.loopers.domain.product;

import static com.loopers.support.error.ErrorType.*;

import java.time.ZonedDateTime;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
	name = "product_counter_event_history",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_product_counter_event_history_dedupe_key", columnNames = {"dedupe_key"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductCounterEventHistory extends BaseEntity {

	private static final int MAX_DEDUPE_KEY_LENGTH = 128;

	@Column(name = "dedupe_key", nullable = false, length = MAX_DEDUPE_KEY_LENGTH)
	private String dedupeKey;

	@Embedded
	private ProductId productId;

	@Enumerated(EnumType.STRING)
	@Column(name = "counter_type", nullable = false, length = 20)
	private ProductCounterType counterType;

	@Enumerated(EnumType.STRING)
	@Column(name = "process_status", nullable = false, length = 20)
	private ProductCounterProcessStatus processStatus;

	@Column(name = "failed_reason", length = 255)
	private String failedReason;

	@Column(name = "processed_at")
	private ZonedDateTime processedAt;

	@Builder(builderMethodName = "create")
	public ProductCounterEventHistory(
		final String dedupeKey,
		final ProductId productId,
		final ProductCounterType counterType,
		final ProductCounterProcessStatus processStatus,
		final String failedReason,
		final ZonedDateTime processedAt
	) {
		this.dedupeKey = normalize(dedupeKey);
		this.productId = productId;
		this.counterType = counterType;
		this.processStatus = processStatus;
		this.failedReason = failedReason;
		this.processedAt = processedAt;
	}

	@Override
	protected void guard() {
		if (dedupeKey == null || dedupeKey.isBlank()) {
			throw new CoreException(BAD_REQUEST, "카운터 멱등 키는 비어있을 수 없습니다.");
		}
		if (dedupeKey.length() > MAX_DEDUPE_KEY_LENGTH) {
			throw new CoreException(BAD_REQUEST, "카운터 멱등 키 길이가 너무 깁니다.");
		}
		if (productId == null) {
			throw new CoreException(BAD_REQUEST, "상품 ID는 필수입니다.");
		}
		if (counterType == null) {
			throw new CoreException(BAD_REQUEST, "카운터 타입은 필수입니다.");
		}
		if (processStatus == null) {
			throw new CoreException(BAD_REQUEST, "카운터 처리 상태는 필수입니다.");
		}
	}

	public void markProcessing() {
		if (processStatus == ProductCounterProcessStatus.COMPLETED) {
			return;
		}
		this.processStatus = ProductCounterProcessStatus.PROCESSING;
	}

	public void complete() {
		this.processStatus = ProductCounterProcessStatus.COMPLETED;
		this.failedReason = null;
		this.processedAt = ZonedDateTime.now();
	}

	public void fail(final String reason) {
		this.processStatus = ProductCounterProcessStatus.FAILED;
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
