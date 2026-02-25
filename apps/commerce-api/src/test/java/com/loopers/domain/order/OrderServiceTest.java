package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.loopers.domain.common.Price;
import com.loopers.domain.common.Quantity;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.user.UserId;

@DisplayName("OrderService 테스트")
class OrderServiceTest {

	@Test
	@DisplayName("주문 목록 조회 시 주문번호 기준으로 주문 아이템을 매핑한다")
	void getOrdersMapsItemsByOrderNumber() {
		OrderRepository orderRepository = mock(OrderRepository.class);
		OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
		OrderService orderService = new OrderService(orderRepository, orderItemRepository);

		Order firstOrder = Order.builder()
			.userId(UserId.of("user1234"))
			.totalOrderPrice(new TotalOrderPrice(1000L))
			.couponDiscountAmount(CouponDiscountAmount.of(0L))
			.finalPaymentAmount(FinalPaymentAmount.of(1000L, 0L))
			.orderNumber(new OrderNumber("ORD-1"))
			.status(OrderStatus.PENDING)
			.build();

		Order secondOrder = Order.builder()
			.userId(UserId.of("user1234"))
			.totalOrderPrice(new TotalOrderPrice(2000L))
			.couponDiscountAmount(CouponDiscountAmount.of(0L))
			.finalPaymentAmount(FinalPaymentAmount.of(2000L, 0L))
			.orderNumber(new OrderNumber("ORD-2"))
			.status(OrderStatus.PENDING)
			.build();

		when(orderRepository.findAllOrdersByUserId("user1234")).thenReturn(List.of(firstOrder, secondOrder));

		OrderItem firstOrderItem = OrderItem.builder()
			.orderId(new OrderId("ORD-1"))
			.productId(ProductId.of(10L))
			.quantity(new Quantity(1L))
			.price(new Price(1000L))
			.build();

		OrderItem secondOrderItem = OrderItem.builder()
			.orderId(new OrderId("ORD-2"))
			.productId(ProductId.of(20L))
			.quantity(new Quantity(2L))
			.price(new Price(1000L))
			.build();

		when(orderItemRepository.findAllByOrderIdIn(List.of("ORD-1", "ORD-2")))
			.thenReturn(List.of(firstOrderItem, secondOrderItem));

		List<OrderInfo> result = orderService.getOrders(new OrderData.GetOrders("user1234"));

		assertThat(result).hasSize(2);
		assertThat(result.get(0).orderId()).isEqualTo("ORD-1");
		assertThat(result.get(0).items()).containsExactly(firstOrderItem);
		assertThat(result.get(1).orderId()).isEqualTo("ORD-2");
		assertThat(result.get(1).items()).containsExactly(secondOrderItem);
	}
}
