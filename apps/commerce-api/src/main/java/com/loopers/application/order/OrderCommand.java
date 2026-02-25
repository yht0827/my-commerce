package com.loopers.application.order;

import java.util.List;

import com.loopers.domain.order.OrderData;
import com.loopers.domain.payment.CardType;

public record OrderCommand() {

	public record CreateOrder(String userId, List<OrderItemCommand> items, Long couponId, CardType cardType, String cardNo,
							  String callbackUrl, String idempotencyKey) {
		public OrderData.CreateOrder toData() {
			List<OrderData.OrderItemData> items = this.items.stream()
				.map(OrderItemCommand::toData)
				.toList();

			return new OrderData.CreateOrder(userId, items, couponId);
		}
	}

	public record OrderItemCommand(Long productId, Long quantity) {
		public OrderData.OrderItemData toData() {
			return new OrderData.OrderItemData(productId, quantity);
		}
	}

}
