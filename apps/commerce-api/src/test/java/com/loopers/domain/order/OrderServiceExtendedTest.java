package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.loopers.domain.common.Price;
import com.loopers.domain.common.Quantity;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.user.UserId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("OrderService 확장 테스트")
class OrderServiceExtendedTest {

	private OrderRepository orderRepository;
	private OrderItemRepository orderItemRepository;
	private OrderService orderService;

	@BeforeEach
	void setUp() {
		orderRepository = mock(OrderRepository.class);
		orderItemRepository = mock(OrderItemRepository.class);
		orderService = new OrderService(orderRepository, orderItemRepository);
	}

	private Order createTestOrder(String orderNum, String userId, Long totalPrice, Long discount, Long couponId, OrderStatus status) {
		return Order.builder()
			.userId(UserId.of(userId))
			.totalOrderPrice(new TotalOrderPrice(totalPrice))
			.couponDiscountAmount(CouponDiscountAmount.of(discount))
			.finalPaymentAmount(FinalPaymentAmount.of(totalPrice, discount))
			.orderNumber(new OrderNumber(orderNum))
			.couponId(couponId)
			.status(status)
			.build();
	}

	@DisplayName("getOrder 메서드")
	@Nested
	class GetOrder {

		@DisplayName("존재하지 않으면 NOT_FOUND 예외가 발생한다")
		@Test
		void throwsNotFoundWhenOrderDoesNotExist() {
			// arrange
			OrderData.GetOrder data = new OrderData.GetOrder("user1234", "ORD-999");
			when(orderRepository.findByOrderNumberAndUserId("ORD-999", "user1234")).thenReturn(Optional.empty());

			// act & assert
			assertThatThrownBy(() -> orderService.getOrder(data))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
		}

		@DisplayName("존재하면 주문 정보를 반환한다")
		@Test
		void returnsOrderInfoWhenExists() {
			// arrange
			Order order = createTestOrder("ORD-1", "user1234", 10000L, 0L, null, OrderStatus.PENDING);
			OrderData.GetOrder data = new OrderData.GetOrder("user1234", "ORD-1");
			when(orderRepository.findByOrderNumberAndUserId("ORD-1", "user1234")).thenReturn(Optional.of(order));

			OrderItem item = OrderItem.builder()
				.orderId(new OrderId("ORD-1"))
				.productId(ProductId.of(1L))
				.quantity(new Quantity(2L))
				.price(new Price(5000L))
				.build();
			when(orderItemRepository.findAllByOrderId("ORD-1")).thenReturn(List.of(item));

			// act
			OrderInfo result = orderService.getOrder(data);

			// assert
			assertThat(result.orderId()).isEqualTo("ORD-1");
			assertThat(result.userId()).isEqualTo("user1234");
			assertThat(result.items()).hasSize(1);
		}
	}

	@DisplayName("getOrders 메서드")
	@Nested
	class GetOrders {

		@DisplayName("빈 목록이면 빈 리스트를 반환한다")
		@Test
		void returnsEmptyListWhenNoOrders() {
			// arrange
			OrderData.GetOrders data = new OrderData.GetOrders("user1234");
			when(orderRepository.findAllOrdersByUserId("user1234")).thenReturn(Collections.emptyList());

			// act
			List<OrderInfo> result = orderService.getOrders(data);

			// assert
			assertThat(result).isEmpty();
		}
	}

	@DisplayName("getCompensationInfo 메서드")
	@Nested
	class GetCompensationInfo {

		@DisplayName("주문이 없으면 NOT_FOUND 예외가 발생한다")
		@Test
		void throwsNotFoundWhenOrderDoesNotExist() {
			// arrange
			when(orderRepository.findByOrderNumber("ORD-999")).thenReturn(Optional.empty());

			// act & assert
			assertThatThrownBy(() -> orderService.getCompensationInfo("ORD-999"))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
		}

		@DisplayName("정상적으로 보상 정보를 반환한다")
		@Test
		void returnsCompensationInfo() {
			// arrange
			Order order = createTestOrder("ORD-1", "user1234", 10000L, 2000L, 5L, OrderStatus.CONFIRMED);
			when(orderRepository.findByOrderNumber("ORD-1")).thenReturn(Optional.of(order));

			// act
			OrderData.CompensationInfo result = orderService.getCompensationInfo("ORD-1");

			// assert
			assertThat(result.userId()).isEqualTo("user1234");
			assertThat(result.finalPaymentAmount()).isEqualTo(8000L);
			assertThat(result.couponId()).isEqualTo(5L);
		}
	}

	@DisplayName("updateOrderStatus 메서드")
	@Nested
	class UpdateOrderStatus {

		@DisplayName("주문이 없으면 NOT_FOUND 예외가 발생한다")
		@Test
		void throwsNotFoundWhenOrderDoesNotExist() {
			// arrange
			when(orderRepository.findByOrderNumber("ORD-999")).thenReturn(Optional.empty());

			// act & assert
			assertThatThrownBy(() -> orderService.updateOrderStatus("ORD-999", OrderStatus.CONFIRMED))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
		}

		@DisplayName("CONFIRMED로 업데이트하면 주문 상태가 변경된다")
		@Test
		void updatesStatusToConfirmed() {
			// arrange
			Order order = createTestOrder("ORD-1", "user1234", 10000L, 0L, null, OrderStatus.PENDING);
			when(orderRepository.findByOrderNumber("ORD-1")).thenReturn(Optional.of(order));

			// act
			orderService.updateOrderStatus("ORD-1", OrderStatus.CONFIRMED);

			// assert
			assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
		}
	}

	@DisplayName("calculateTotalOrderPrice 메서드")
	@Nested
	class CalculateTotalOrderPrice {

		@DisplayName("여러 아이템의 합계를 계산한다")
		@Test
		void calculatesTotal() {
			// arrange
			OrderItem item1 = OrderItem.builder()
				.orderId(new OrderId("ORD-1"))
				.productId(ProductId.of(1L))
				.quantity(new Quantity(2L))
				.price(new Price(1000L))
				.build();

			OrderItem item2 = OrderItem.builder()
				.orderId(new OrderId("ORD-1"))
				.productId(ProductId.of(2L))
				.quantity(new Quantity(3L))
				.price(new Price(500L))
				.build();

			// act
			TotalOrderPrice result = orderService.calculateTotalOrderPrice(List.of(item1, item2));

			// assert
			assertThat(result.getTotalPrice()).isEqualTo(3500L);
		}
	}
}
