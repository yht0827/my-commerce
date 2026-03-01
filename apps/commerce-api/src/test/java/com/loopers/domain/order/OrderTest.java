package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Order 도메인 테스트")
class OrderTest {

	@Test
	@DisplayName("주문 생성 시 주문번호가 자동으로 생성된다")
	void createOrderGeneratesOrderNumber() {
		OrderData.CreateOrder data = new OrderData.CreateOrder(
			"user1234",
			List.of(new OrderData.OrderItemData(1L, 1L)),
			null
		);

		Order order = Order.create(data, new TotalOrderPrice(10000L), CouponDiscountAmount.of(1500L));

		assertThat(order.getOrderNumber()).isNotNull();
		assertThat(order.getOrderNumber().getOrderNumber()).startsWith("ORD-");
		assertThat(order.getFinalPaymentAmount().getFinalPaymentAmount()).isEqualTo(8500L);
	}
}
