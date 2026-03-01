package com.loopers.application.order;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loopers.application.outbox.OutboxPayloadMapper;
import com.loopers.domain.order.OrderData;
import com.loopers.domain.order.OrderHistory;
import com.loopers.domain.order.OrderHistoryRepository;
import com.loopers.domain.order.OrderInfo;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.outbox.OutboxEventType;
import com.loopers.domain.outbox.OutboxService;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.user.UserId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.support.event.EventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderApplicationService 테스트")
class OrderApplicationServiceTest {

	private static final String USER_ID = "orderuser1";
	private static final String IDEMPOTENCY_KEY = "idem-key-1";

	@Mock
	private OrderService orderService;

	@Mock
	private OrderHistoryRepository orderHistoryRepository;

	@Mock
	private OrderProcessor orderProcessor;

	@Mock
	private OutboxService outboxService;

	@Mock
	private OutboxPayloadMapper outboxPayloadMapper;

	@Mock
	private EventPublisher eventPublisher;

	@InjectMocks
	private OrderApplicationService orderApplicationService;

	@Test
	@DisplayName("새 멱등성 키면 주문을 생성하고 이력을 완료 처리한다")
	void createOrder_claimedKey_processesAndCompletesHistory() {
		OrderCommand.CreateOrder command = createOrderCommand(IDEMPOTENCY_KEY);
		OrderInfo orderInfo = new OrderInfo("ORD-1", USER_ID, 10000L, OrderStatus.PENDING, List.of());
		OrderHistory history = OrderHistory.create()
			.userId(UserId.of(USER_ID))
			.idempotencyKey(IDEMPOTENCY_KEY)
			.build();

		when(orderHistoryRepository.createIfNotExists(USER_ID, IDEMPOTENCY_KEY)).thenReturn(true);
		when(orderHistoryRepository.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY)).thenReturn(Optional.of(history));
		when(orderProcessor.process(command)).thenReturn(new OrderProcessResult(orderInfo, List.of(), 9000L));
		when(outboxPayloadMapper.write(any())).thenReturn("{}");
		when(outboxService.enqueue(any(), anyString(), anyString(), anyString())).thenReturn(true);

		OrderResult result = orderApplicationService.createOrder(command);

		assertThat(result.orderId()).isEqualTo("ORD-1");
		assertThat(history.getOrderId()).isEqualTo("ORD-1");
		verify(orderProcessor, times(1)).process(command);
		verify(orderHistoryRepository, times(1)).createIfNotExists(USER_ID, IDEMPOTENCY_KEY);
		verify(orderHistoryRepository, times(1)).findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY);
		verify(outboxService, times(1))
			.enqueue(eq(OutboxEventType.PAYMENT_REQUEST), eq("ORD-1"), eq("payment-request:ORD-1"), eq("{}"));
		verify(outboxService, times(1))
			.enqueue(eq(OutboxEventType.DATA_PLATFORM_DISPATCH), eq("ORD-1"), eq("data-platform:ORDER_CREATED:ORD-1"),
				eq("{}"));
		verifyNoInteractions(eventPublisher);
	}

	@Test
	@DisplayName("이미 완료된 멱등성 키면 기존 주문을 재조회해서 반환한다")
	void createOrder_duplicateCompletedKey_returnsExistingOrder() {
		OrderCommand.CreateOrder command = createOrderCommand(IDEMPOTENCY_KEY);
		OrderHistory history = OrderHistory.create()
			.userId(UserId.of(USER_ID))
			.idempotencyKey(IDEMPOTENCY_KEY)
			.orderId("ORD-1")
			.build();
		OrderInfo existingOrder = new OrderInfo("ORD-1", USER_ID, 10000L, OrderStatus.PENDING, List.of());

		when(orderHistoryRepository.createIfNotExists(USER_ID, IDEMPOTENCY_KEY)).thenReturn(false);
		when(orderHistoryRepository.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY)).thenReturn(Optional.of(history));
		when(orderService.getOrder(new OrderData.GetOrder(USER_ID, "ORD-1"))).thenReturn(existingOrder);

		OrderResult result = orderApplicationService.createOrder(command);

		assertThat(result.orderId()).isEqualTo("ORD-1");
		verify(orderProcessor, never()).process(any());
		verify(orderHistoryRepository, times(1)).findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY);
		verifyNoInteractions(outboxService, outboxPayloadMapper);
		verifyNoInteractions(eventPublisher);
	}

	@Test
	@DisplayName("처리 중인 멱등성 키면 충돌 예외를 던진다")
	void createOrder_duplicateInProgress_throwsConflict() {
		OrderCommand.CreateOrder command = createOrderCommand(IDEMPOTENCY_KEY);
		OrderHistory inProgressHistory = OrderHistory.create()
			.userId(UserId.of(USER_ID))
			.idempotencyKey(IDEMPOTENCY_KEY)
			.build();

		when(orderHistoryRepository.createIfNotExists(USER_ID, IDEMPOTENCY_KEY)).thenReturn(false);
		when(orderHistoryRepository.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY))
			.thenReturn(Optional.of(inProgressHistory));

		assertThatThrownBy(() -> orderApplicationService.createOrder(command))
			.isInstanceOf(CoreException.class)
			.satisfies(throwable -> {
				CoreException coreException = (CoreException) throwable;
				assertThat(coreException.getErrorType()).isEqualTo(ErrorType.CONFLICT);
			});

		verify(orderProcessor, never()).process(any());
		verify(orderHistoryRepository, times(1)).findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY);
		verifyNoInteractions(outboxService, outboxPayloadMapper);
	}

	private OrderCommand.CreateOrder createOrderCommand(final String idempotencyKey) {
		return new OrderCommand.CreateOrder(
			USER_ID,
			List.of(new OrderCommand.OrderItemCommand(1L, 2L)),
			null,
			CardType.KB,
			"1111-2222-3333-4444",
			"https://callback.test/orders",
			idempotencyKey
		);
	}
}
