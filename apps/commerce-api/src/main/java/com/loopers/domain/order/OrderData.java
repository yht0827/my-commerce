package com.loopers.domain.order;

import java.util.List;

public record OrderData() {

	public record CreateOrder(String userId, List<OrderItemData> items, Long couponId) {
	}

	public record OrderItemData(Long productId, Long quantity) {
	}

	public record GetOrder(String userId, String orderId) {
	}

	public record GetOrders(String userId) {
	}

	public record CompensationInfo(String userId, Long finalPaymentAmount, Long couponId) {
	}
}
