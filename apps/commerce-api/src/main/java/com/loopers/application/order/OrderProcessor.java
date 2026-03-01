package com.loopers.application.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.loopers.domain.common.Price;
import com.loopers.domain.common.Quantity;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.CouponDiscountAmount;
import com.loopers.domain.order.FinalPaymentAmount;
import com.loopers.domain.order.OrderData;
import com.loopers.domain.order.OrderInfo;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.TotalOrderPrice;
import com.loopers.domain.point.Balance;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.ProductData;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.product.ProductInfo;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductStockService;
import com.loopers.domain.user.UserId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorMessage;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderProcessor {

	private final OrderService orderService;
	private final ProductStockService productStockService;
	private final ProductRepository productRepository;
	private final PointService pointService;
	private final CouponService couponService;

	public OrderProcessResult process(final OrderCommand.CreateOrder command) {
		OrderData.CreateOrder data = command.toData();

		List<OrderData.OrderItemData> itemDataList = data.items();

		List<ProductData.StockQuantityChanged> quantityChanges = productStockService.deductStock(itemDataList);
		List<OrderItem> items = resolveOrderItemsWithCurrentPrice(itemDataList);

		TotalOrderPrice totalOrderPrice = orderService.calculateTotalOrderPrice(items);

		CouponDiscountAmount couponDiscountAmount = couponService.applyDiscount(data.couponId(), totalOrderPrice);
		FinalPaymentAmount finalPaymentAmount = FinalPaymentAmount.of(
			totalOrderPrice.getTotalPrice(),
			couponDiscountAmount.getCouponDiscountAmount()
		);
		pointService.use(
			UserId.of(data.userId()),
			Balance.of(BigDecimal.valueOf(finalPaymentAmount.getFinalPaymentAmount()))
		);

		OrderInfo orderInfo = orderService.createOrder(data, items, totalOrderPrice, couponDiscountAmount);

		return new OrderProcessResult(orderInfo, quantityChanges, finalPaymentAmount.getFinalPaymentAmount());
	}

	private List<OrderItem> resolveOrderItemsWithCurrentPrice(final List<OrderData.OrderItemData> itemDataList) {
		Map<Long, Long> priceByProductId = productRepository.findInfosByIds(
			itemDataList.stream()
				.map(itemData -> ProductId.of(itemData.productId()))
				.toList()
		).stream().collect(Collectors.toMap(ProductInfo::productId, ProductInfo::price, (first, ignored) -> first));

		return itemDataList.stream()
			.map(itemData -> {
				Long price = priceByProductId.get(itemData.productId());
				if (price == null) {
					throw new CoreException(ErrorType.NOT_FOUND, ErrorMessage.PRODUCT_NOT_FOUND.format(itemData.productId()));
				}

				return OrderItem.builder()
					.productId(ProductId.of(itemData.productId()))
					.quantity(new Quantity(itemData.quantity()))
					.price(new Price(price))
					.build();
			})
			.toList();
	}

}
