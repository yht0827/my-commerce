package com.loopers.application.order;

import java.util.List;

import org.springframework.stereotype.Component;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.CouponDiscountAmount;
import com.loopers.domain.order.OrderData;
import com.loopers.domain.order.OrderInfo;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.TotalOrderPrice;
import com.loopers.domain.product.ProductData;
import com.loopers.domain.product.ProductStockService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderProcessor {

	private final OrderService orderService;
	private final ProductStockService productStockService;
	private final CouponService couponService;

	public OrderProcessResult process(final OrderCommand.CreateOrder command) {
		OrderData.CreateOrder data = command.toData();

		List<OrderItem> items = data.items();

		List<ProductData.StockQuantityChanged> quantityChanges = productStockService.deductStock(items);

		TotalOrderPrice totalOrderPrice = orderService.calculateTotalOrderPrice(items);

		CouponDiscountAmount couponDiscountAmount = couponService.applyDiscount(data.couponId(), totalOrderPrice);
		OrderInfo orderInfo = orderService.createOrder(data, items, totalOrderPrice, couponDiscountAmount);

		return new OrderProcessResult(orderInfo, quantityChanges);
	}

}
