package com.loopers.domain.payment;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.order.OrderId;
import com.loopers.domain.common.Price;
import com.loopers.domain.user.UserId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

	@Embedded
	private OrderId orderId;

	@Embedded
	private UserId userId;

	@Enumerated(EnumType.STRING)
	private CardType cardType;

	@Embedded
	private CardNo cardNo;

	@Embedded
	private Price amount;

	@Embedded
	private CallbackUrl callbackUrl;

	@Embedded
	private TransactionKey transactionKey;

	@Enumerated(EnumType.STRING)
	private TransactionStatus status;

	@Embedded
	private PaymentReason reason;

	@Version
	private Long version;

	@Builder
	public Payment(OrderId orderId, UserId userId, CardType cardType, CardNo cardNo, Price amount, CallbackUrl callbackUrl,
		TransactionKey transactionKey, TransactionStatus status, PaymentReason reason) {
		this.orderId = orderId;
		this.userId = userId;
		this.cardType = cardType;
		this.cardNo = cardNo;
		this.amount = amount;
		this.callbackUrl = callbackUrl;
		this.transactionKey = transactionKey;
		this.status = status;
		this.reason = reason;
	}

	public void updateTransactionKey(final String orderId) {
		if (orderId == null || orderId.isBlank()) {
			throw new CoreException(ErrorType.BAD_REQUEST, "주문 ID는 비어있을 수 없습니다.");
		}

		this.transactionKey = new TransactionKey(orderId);
	}

	public void updatePaymentStatus(TransactionStatus newStatus) {
		if (status == TransactionStatus.SUCCESS) {
			throw new CoreException(ErrorType.BAD_REQUEST, "이미 완료된 결제는 변경할 수 없습니다.");
		}

		this.status = newStatus;
	}

	public void processPaymentSuccess() {
		updatePaymentStatus(TransactionStatus.SUCCESS);
	}

	public void processPaymentPending() {
		updatePaymentStatus(TransactionStatus.PENDING);
	}

	public void processPaymentFailed() {
		updatePaymentStatus(TransactionStatus.FAILED);
	}

}
