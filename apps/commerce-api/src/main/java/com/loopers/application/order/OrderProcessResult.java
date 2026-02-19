package com.loopers.application.order;

import java.util.List;

import com.loopers.domain.order.OrderInfo;
import com.loopers.domain.product.ProductData;

public record OrderProcessResult(
	OrderInfo orderInfo,
	List<ProductData.StockQuantityChanged> quantityChanges
) {
}
