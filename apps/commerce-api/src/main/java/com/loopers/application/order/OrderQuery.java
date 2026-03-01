package com.loopers.application.order;

import com.loopers.domain.order.OrderData;

public record OrderQuery() {

	public record GetOrders(String userId) {

		public OrderData.GetOrders toData() {
			return new OrderData.GetOrders(userId);
		}

	}

	public record GetOrder(String userId, String orderId) {
		public OrderData.GetOrder toData() {
			return new OrderData.GetOrder(userId, orderId);
		}
	}
}
