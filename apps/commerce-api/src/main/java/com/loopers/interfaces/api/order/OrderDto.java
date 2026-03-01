package com.loopers.interfaces.api.order;

import java.util.List;

import com.loopers.application.order.OrderCommand;
import com.loopers.application.order.OrderQuery;
import com.loopers.application.order.OrderResult;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.CardType;

public record OrderDto() {

	public record V1() {

		public record OrderRequest(List<OrderItemRequest> items, Long couponId, CardType cardType, String cardNo,
								   String callbackUrl) {
			public OrderCommand.CreateOrder toCommand(final String userId, final String idempotencyKey) {
				List<OrderCommand.OrderItemCommand> items = this.items.stream()
					.map(OrderItemRequest::toCommand)
					.toList();

				return new OrderCommand.CreateOrder(userId, items, couponId, cardType, cardNo, callbackUrl, idempotencyKey);
			}
		}

		public record OrderItemRequest(Long productId, Long quantity) {
			public OrderCommand.OrderItemCommand toCommand() {
				return new OrderCommand.OrderItemCommand(productId, quantity);
			}
		}

		public record GetOrdersRequest(String userId) {
			public static OrderQuery.GetOrders toQuery(final String userId) {
				return new OrderQuery.GetOrders(userId);
			}
		}

		public record GetOrderRequest(String userId, String orderId) {
			public static OrderQuery.GetOrder toQuery(final String userId, final String orderId) {
				return new OrderQuery.GetOrder(userId, orderId);
			}
		}

		public record OrderResponse(String orderId, String userId, Long totalPrice, OrderStatus status) {
			public static OrderResponse from(OrderResult orderResult) {
				return new OrderResponse(orderResult.orderId(), orderResult.userId(), orderResult.totalPrice(),
					orderResult.status());
			}
		}

	}
}
