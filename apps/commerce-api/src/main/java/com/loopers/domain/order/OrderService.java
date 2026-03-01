package com.loopers.domain.order;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;

	public TotalOrderPrice calculateTotalOrderPrice(List<OrderItem> orderItems) {
		return TotalOrderPrice.of(orderItems);
	}

	public OrderInfo createOrder(OrderData.CreateOrder data, List<OrderItem> orderItems, TotalOrderPrice totalOrderPrice,
		CouponDiscountAmount couponDiscountAmount) {

		Order order = Order.create(data, totalOrderPrice, couponDiscountAmount);
		Order savedOrder = orderRepository.save(order);
		OrderId orderId = new OrderId(savedOrder.getOrderNumber().getOrderNumber());
		List<OrderItem> orderItemsWithOrderId = orderItems.stream()
			.map(orderItem -> OrderItem.builder()
				.orderId(orderId)
				.productId(orderItem.getProductId())
				.quantity(orderItem.getQuantity())
				.price(orderItem.getPrice())
				.build())
			.toList();
		List<OrderItem> orderItemList = orderItemRepository.saveAll(orderItemsWithOrderId);

		return OrderInfo.from(savedOrder, orderItemList);
	}

	@Transactional(readOnly = true)
	public List<OrderInfo> getOrders(final OrderData.GetOrders data) {
		List<Order> orderList = orderRepository.findAllOrdersByUserId(data.userId());

		if (orderList.isEmpty()) {
			return Collections.emptyList();
		}

		List<String> orderIds = orderList.stream()
			.map(order -> order.getOrderNumber().getOrderNumber())
			.toList();

		List<OrderItem> allOrderItems = orderItemRepository.findAllByOrderIdIn(orderIds);

		Map<String, List<OrderItem>> orderItemMap = allOrderItems.stream()
			.collect(Collectors.groupingBy(orderItem -> orderItem.getOrderId().getOrderId()));

		return orderList.stream().map(order -> {
			List<OrderItem> orderItems = orderItemMap.getOrDefault(order.getOrderNumber().getOrderNumber(),
				Collections.emptyList());
			return OrderInfo.from(order, orderItems);
		}).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public OrderInfo getOrder(final OrderData.GetOrder data) {
		Order order = orderRepository.findByOrderNumberAndUserId(data.orderId(), data.userId())
			.orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

		List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(order.getOrderNumber().getOrderNumber());

		return OrderInfo.from(order, orderItems);
	}

	@Transactional(readOnly = true)
	public List<OrderItem> getOrderItems(final String orderId) {
		return orderItemRepository.findAllByOrderId(orderId);
	}

	@Transactional(readOnly = true)
	public OrderData.CompensationInfo getCompensationInfo(final String orderId) {
		Order order = orderRepository.findByOrderNumber(orderId)
			.orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
		return new OrderData.CompensationInfo(
			order.getUserId().getUserId(),
			order.getFinalPaymentAmount().getFinalPaymentAmount(),
			order.getCouponId()
		);
	}

	@Transactional
	public void updateOrderStatus(final String orderId, final OrderStatus status) {
		Order order = orderRepository.findByOrderNumber(orderId)
			.orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

		switch (status) {
			case CONFIRMED -> order.processPaymentSuccess();
			case CANCELLED -> order.processPaymentFailed();
		}

	}

}
