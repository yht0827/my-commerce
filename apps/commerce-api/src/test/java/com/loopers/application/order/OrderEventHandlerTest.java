package com.loopers.application.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.event.PaymentCompletedEvent;
import com.loopers.domain.product.ProductId;
import com.loopers.domain.product.event.ProductOrderedEvent;
import com.loopers.support.event.Envelope;
import com.loopers.support.event.Event;
import com.loopers.support.event.EventPublisher;

@DisplayName("OrderEventHandler 테스트")
class OrderEventHandlerTest {

	private final OrderService orderService = mock(OrderService.class);
	private final EventPublisher eventPublisher = mock(EventPublisher.class);

	private final OrderEventHandler orderEventHandler = new OrderEventHandler(orderService, eventPublisher);

	@Test
	@DisplayName("결제 완료 후 주문 상품별 PRODUCT_ORDERED 이벤트를 중복 없이 발행한다")
	void processPaymentCompleted_publishesProductOrderedEventsByUniqueProduct() {
		PaymentCompletedEvent completedEvent = PaymentCompletedEvent.create("ORD-1001", "TR-1001");
		Envelope<PaymentCompletedEvent> envelope = Envelope.of(completedEvent);

		OrderItem first = mock(OrderItem.class);
		OrderItem duplicated = mock(OrderItem.class);
		OrderItem second = mock(OrderItem.class);

		when(first.getProductId()).thenReturn(ProductId.of(1L));
		when(duplicated.getProductId()).thenReturn(ProductId.of(1L));
		when(second.getProductId()).thenReturn(ProductId.of(2L));

		when(orderService.getOrderItems("ORD-1001")).thenReturn(List.of(first, duplicated, second));

		orderEventHandler.processPaymentCompleted(envelope);

		verify(orderService).updateOrderStatus("ORD-1001", OrderStatus.CONFIRMED);
		verify(orderService).getOrderItems("ORD-1001");

		ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
		verify(eventPublisher, times(2)).publish(captor.capture());

		List<Event> publishedEvents = captor.getAllValues();
		assertThat(publishedEvents)
			.hasSize(2)
			.allMatch(ProductOrderedEvent.class::isInstance);

		List<ProductOrderedEvent> productOrderedEvents = publishedEvents.stream()
			.map(ProductOrderedEvent.class::cast)
			.toList();

		assertThat(productOrderedEvents)
			.extracting(ProductOrderedEvent::orderId)
			.containsOnly("ORD-1001");
		assertThat(productOrderedEvents)
			.extracting(ProductOrderedEvent::productId)
			.containsExactlyInAnyOrder(1L, 2L);
	}

	@Test
	@DisplayName("주문 상품이 없으면 PRODUCT_ORDERED 이벤트를 발행하지 않는다")
	void processPaymentCompleted_skipsWhenNoOrderItems() {
		PaymentCompletedEvent completedEvent = PaymentCompletedEvent.create("ORD-1002", "TR-1002");
		Envelope<PaymentCompletedEvent> envelope = Envelope.of(completedEvent);

		when(orderService.getOrderItems("ORD-1002")).thenReturn(List.of());

		orderEventHandler.processPaymentCompleted(envelope);

		verify(orderService).updateOrderStatus("ORD-1002", OrderStatus.CONFIRMED);
		verify(orderService).getOrderItems("ORD-1002");
		verify(eventPublisher, never()).publish(any());
	}
}
