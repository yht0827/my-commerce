package com.loopers.application.order;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.order.OrderData;
import com.loopers.domain.order.OrderInfo;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.event.OrderCreatedEvent;
import com.loopers.domain.product.ProductData;
import com.loopers.domain.product.event.ProductQuantityChangedEvent;
import com.loopers.support.event.EventPublisher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderFacade {

	private final OrderService orderService;
	private final OrderProcessor orderProcessor;
	private final EventPublisher eventPublisher;

	@Transactional
	public OrderResult createOrder(final OrderCommand.CreateOrder command) {
		OrderProcessResult processResult = orderProcessor.process(command);
		publishQuantityChangedEvents(processResult.quantityChanges());

		OrderInfo orderInfo = processResult.orderInfo();

		OrderCreatedEvent.PaymentMetadata paymentMetadata = OrderCreatedEvent.PaymentMetadata.of(
			command.cardType(),
			command.cardNo(),
			command.callbackUrl()
		);

		OrderCreatedEvent orderCreatedEvent = OrderCreatedEvent.from(orderInfo, paymentMetadata);
		eventPublisher.publish(orderCreatedEvent);

		return OrderResult.from(orderInfo);
	}

	private void publishQuantityChangedEvents(final List<ProductData.StockQuantityChanged> quantityChanges) {
		for (ProductData.StockQuantityChanged quantityChange : quantityChanges) {
			eventPublisher.publish(ProductQuantityChangedEvent.create(
				quantityChange.productId(),
				quantityChange.previousQuantity(),
				quantityChange.currentQuantity()
			));
		}
	}

	public List<OrderResult> getOrders(final OrderQuery.GetOrders command) {
		OrderData.GetOrders orderData = command.toData();
		List<OrderInfo> orderList = orderService.getOrders(orderData);
		return orderList.stream().map(OrderResult::from).toList();
	}

	public OrderResult getOrder(final OrderQuery.GetOrder command) {
		OrderData.GetOrder orderData = command.toData();
		OrderInfo orderInfo = orderService.getOrder(orderData);
		return OrderResult.from(orderInfo);
	}

}
