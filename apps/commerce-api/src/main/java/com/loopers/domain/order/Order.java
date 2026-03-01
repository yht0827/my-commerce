package com.loopers.domain.order;

import java.util.UUID;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.UserId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

	@Embedded
	private UserId userId;

	@Embedded
	private TotalOrderPrice totalOrderPrice;

	@Embedded
	private CouponDiscountAmount couponDiscountAmount;

	@Embedded
	private FinalPaymentAmount finalPaymentAmount;

	@Embedded
	private OrderNumber orderNumber;

	@Column(name = "coupon_id")
	private Long couponId;

	@Enumerated(EnumType.STRING)
	private OrderStatus status;

	@Builder
	public Order(UserId userId, TotalOrderPrice totalOrderPrice, CouponDiscountAmount couponDiscountAmount,
		FinalPaymentAmount finalPaymentAmount, OrderNumber orderNumber, Long couponId, OrderStatus status) {
		this.userId = userId;
		this.totalOrderPrice = totalOrderPrice;
		this.couponDiscountAmount = couponDiscountAmount;
		this.finalPaymentAmount = finalPaymentAmount;
		this.orderNumber = orderNumber;
		this.couponId = couponId;
		this.status = status;
	}

	public static Order create(OrderData.CreateOrder data, TotalOrderPrice totalOrderPrice,
		CouponDiscountAmount couponDiscountAmount) {

		Long totalPrice = totalOrderPrice.getTotalPrice();
		Long discountAmount = couponDiscountAmount.getCouponDiscountAmount();
		FinalPaymentAmount finalPaymentAmount = FinalPaymentAmount.of(totalPrice, discountAmount);

		return Order.builder()
			.userId(new UserId(data.userId()))
			.totalOrderPrice(totalOrderPrice)
			.couponDiscountAmount(couponDiscountAmount)
			.finalPaymentAmount(finalPaymentAmount)
			.orderNumber(generateOrderNumber())
			.couponId(data.couponId())
			.status(OrderStatus.PENDING)
			.build();
	}

	private static OrderNumber generateOrderNumber() {
		return new OrderNumber("ORD-" + UUID.randomUUID());
	}

	public void updateOrderStatus(OrderStatus newStatus) {
		if (status == newStatus) {
			return;
		}

		if (status == OrderStatus.CONFIRMED) {
			throw new CoreException(ErrorType.BAD_REQUEST, "이미 완료된 주문은 변경할 수 없습니다.");
		}
		if (status == OrderStatus.CANCELLED) {
			throw new CoreException(ErrorType.BAD_REQUEST, "취소된 주문은 변경할 수 없습니다.");
		}

		this.status = newStatus;
	}

	public void processPaymentSuccess() {
		updateOrderStatus(OrderStatus.CONFIRMED);
	}

	public void processPaymentFailed() {
		updateOrderStatus(OrderStatus.CANCELLED);
	}

}
