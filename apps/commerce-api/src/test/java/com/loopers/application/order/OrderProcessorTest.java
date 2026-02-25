package com.loopers.application.order;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.CouponDiscountAmount;
import com.loopers.domain.order.OrderData;
import com.loopers.domain.order.OrderInfo;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.TotalOrderPrice;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.point.Balance;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.ProductData;
import com.loopers.domain.product.ProductInfo;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductStockService;
import com.loopers.domain.user.UserId;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderProcessor 테스트")
class OrderProcessorTest {

	@Mock
	private OrderService orderService;

	@Mock
	private ProductStockService productStockService;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private PointService pointService;

	@Mock
	private CouponService couponService;

	@InjectMocks
	private OrderProcessor orderProcessor;

	@Test
	@DisplayName("주문 처리 시 최종 결제 금액만큼 포인트를 차감한다")
	void process_deductsPointByFinalPaymentAmount() {
		OrderCommand.CreateOrder command = new OrderCommand.CreateOrder(
			"orderuser1",
			List.of(new OrderCommand.OrderItemCommand(1L, 2L)),
			null,
			CardType.KB,
			"1111-2222-3333-4444",
			"https://callback.test/orders",
			null
		);

		when(productStockService.deductStock(anyList()))
			.thenReturn(List.of(new ProductData.StockQuantityChanged(1L, 10L, 8L)));
		when(productRepository.findInfosByIds(anyList()))
			.thenReturn(List.of(new ProductInfo(1L, "product-1", 5000L, 10L, "brand-1", 0L, 0L, 0L)));

		TotalOrderPrice totalOrderPrice = new TotalOrderPrice(10000L);
		CouponDiscountAmount couponDiscountAmount = CouponDiscountAmount.of(1000L);

		when(orderService.calculateTotalOrderPrice(anyList())).thenReturn(totalOrderPrice);
		when(couponService.applyDiscount(eq(null), eq(totalOrderPrice))).thenReturn(couponDiscountAmount);
		when(orderService.createOrder(any(OrderData.CreateOrder.class), anyList(), eq(totalOrderPrice), eq(couponDiscountAmount)))
			.thenReturn(new OrderInfo("ORD-1", "orderuser1", 10000L, OrderStatus.PENDING, List.of()));

		OrderProcessResult result = orderProcessor.process(command);

		ArgumentCaptor<UserId> userIdCaptor = ArgumentCaptor.forClass(UserId.class);
		ArgumentCaptor<Balance> balanceCaptor = ArgumentCaptor.forClass(Balance.class);
		verify(pointService, times(1)).use(userIdCaptor.capture(), balanceCaptor.capture());

		assertThat(userIdCaptor.getValue().getUserId()).isEqualTo("orderuser1");
		assertThat(balanceCaptor.getValue().getBalance()).isEqualByComparingTo(BigDecimal.valueOf(9000L));
		assertThat(result.orderInfo().orderId()).isEqualTo("ORD-1");
		assertThat(result.quantityChanges()).hasSize(1);
	}
}
