package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.loopers.domain.user.UserId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@DisplayName("Order 도메인 확장 테스트")
class OrderDomainExtendedTest {

	private Order createPendingOrder() {
		return Order.builder()
			.userId(UserId.of("user1234"))
			.totalOrderPrice(new TotalOrderPrice(10000L))
			.couponDiscountAmount(CouponDiscountAmount.of(0L))
			.finalPaymentAmount(FinalPaymentAmount.of(10000L, 0L))
			.orderNumber(new OrderNumber("ORD-TEST"))
			.status(OrderStatus.PENDING)
			.build();
	}

	private Order createOrderWithStatus(OrderStatus status) {
		return Order.builder()
			.userId(UserId.of("user1234"))
			.totalOrderPrice(new TotalOrderPrice(10000L))
			.couponDiscountAmount(CouponDiscountAmount.of(0L))
			.finalPaymentAmount(FinalPaymentAmount.of(10000L, 0L))
			.orderNumber(new OrderNumber("ORD-TEST"))
			.status(status)
			.build();
	}

	@DisplayName("updateOrderStatus 메서드")
	@Nested
	class UpdateOrderStatus {

		@DisplayName("동일 상태로 변경하면 아무것도 변경되지 않는다")
		@Test
		void noOpWhenSameStatus() {
			// arrange
			Order order = createPendingOrder();

			// act
			order.updateOrderStatus(OrderStatus.PENDING);

			// assert
			assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
		}

		@DisplayName("PENDING에서 CONFIRMED로 변경된다")
		@Test
		void pendingToConfirmed() {
			// arrange
			Order order = createPendingOrder();

			// act
			order.updateOrderStatus(OrderStatus.CONFIRMED);

			// assert
			assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
		}

		@DisplayName("PENDING에서 CANCELLED로 변경된다")
		@Test
		void pendingToCancelled() {
			// arrange
			Order order = createPendingOrder();

			// act
			order.updateOrderStatus(OrderStatus.CANCELLED);

			// assert
			assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
		}

		@DisplayName("CONFIRMED 상태에서 변경 시도하면 예외가 발생한다")
		@Test
		void throwsExceptionWhenConfirmed() {
			// arrange
			Order order = createOrderWithStatus(OrderStatus.CONFIRMED);

			// act & assert
			assertThatThrownBy(() -> order.updateOrderStatus(OrderStatus.CANCELLED))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
		}

		@DisplayName("CANCELLED 상태에서 변경 시도하면 예외가 발생한다")
		@Test
		void throwsExceptionWhenCancelled() {
			// arrange
			Order order = createOrderWithStatus(OrderStatus.CANCELLED);

			// act & assert
			assertThatThrownBy(() -> order.updateOrderStatus(OrderStatus.CONFIRMED))
				.isInstanceOf(CoreException.class)
				.satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
		}
	}

	@DisplayName("processPaymentSuccess 메서드")
	@Nested
	class ProcessPaymentSuccess {

		@DisplayName("상태가 CONFIRMED로 변경된다")
		@Test
		void changesStatusToConfirmed() {
			// arrange
			Order order = createPendingOrder();

			// act
			order.processPaymentSuccess();

			// assert
			assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
		}
	}

	@DisplayName("processPaymentFailed 메서드")
	@Nested
	class ProcessPaymentFailed {

		@DisplayName("상태가 CANCELLED로 변경된다")
		@Test
		void changesStatusToCancelled() {
			// arrange
			Order order = createPendingOrder();

			// act
			order.processPaymentFailed();

			// assert
			assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
		}
	}
}
