package com.loopers.domain.order;

import static com.loopers.support.error.ErrorMessage.*;
import static com.loopers.support.error.ErrorType.*;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.UserId;
import com.loopers.support.error.CoreException;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
	name = "order_history",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_order_history_user_idempotency_key", columnNames = {"user_id", "idempotency_key"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderHistory extends BaseEntity {

	private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;

	@Embedded
	private UserId userId;

	@Column(name = "idempotency_key", nullable = false, length = MAX_IDEMPOTENCY_KEY_LENGTH)
	private String idempotencyKey;

	@Column(name = "order_id", length = 50)
	private String orderId;

	@Builder(builderMethodName = "create")
	public OrderHistory(final UserId userId, final String idempotencyKey, final String orderId) {
		this.userId = userId;
		this.idempotencyKey = normalizeIdempotencyKey(idempotencyKey);
		this.orderId = orderId;
	}

	@Override
	protected void guard() {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw new CoreException(BAD_REQUEST, ORDER_IDEMPOTENCY_KEY_REQUIRED.format());
		}
		if (idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
			throw new CoreException(BAD_REQUEST, ORDER_IDEMPOTENCY_KEY_TOO_LONG.format(MAX_IDEMPOTENCY_KEY_LENGTH));
		}
		if (orderId != null && !orderId.isBlank()) {
			new OrderId(orderId);
		}
	}

	public boolean isCompleted() {
		return orderId != null && !orderId.isBlank();
	}

	public String getOrderId() {
		return orderId;
	}

	public void complete(final String orderId) {
		new OrderId(orderId);
		this.orderId = orderId;
	}

	private static String normalizeIdempotencyKey(final String idempotencyKey) {
		if (idempotencyKey == null) {
			return null;
		}
		return idempotencyKey.trim();
	}
}
