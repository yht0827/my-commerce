package com.loopers.interfaces.api.order;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.order.OrderCommand;
import com.loopers.application.order.OrderApplicationService;
import com.loopers.application.order.OrderResult;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.CardType;
import com.loopers.interfaces.api.common.CurrentUserIdArgumentResolver;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderV1Controller 테스트")
class OrderV1ControllerTest {

	private static final String USER_ID = "orderuser1";

	@Mock
	private OrderApplicationService orderApplicationService;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		OrderV1Controller controller = new OrderV1Controller(orderApplicationService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
			.setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
			.build();
		objectMapper = new ObjectMapper();
	}

	@Test
	@DisplayName("POST /api/v1/orders - 주문 생성 응답을 반환한다")
	void createOrderReturnsOrderResponse() throws Exception {
		OrderDto.V1.OrderRequest request = new OrderDto.V1.OrderRequest(
			List.of(new OrderDto.V1.OrderItemRequest(1L, 2L)),
			null,
			CardType.KB,
			"1111-2222-3333-4444",
			"https://callback.test/orders"
		);

		when(orderApplicationService.createOrder(any()))
			.thenReturn(new OrderResult("ORD-1", USER_ID, 10000L, OrderStatus.PENDING));

		mockMvc.perform(post("/api/v1/orders")
				.header("X-USER-ID", USER_ID)
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.meta.result").value("SUCCESS"))
			.andExpect(jsonPath("$.data.orderId").value("ORD-1"))
			.andExpect(jsonPath("$.data.userId").value(USER_ID))
			.andExpect(jsonPath("$.data.totalPrice").value(10000L))
			.andExpect(jsonPath("$.data.status").value("PENDING"));

		verify(orderApplicationService, times(1)).createOrder(any());
	}

	@Test
	@DisplayName("POST /api/v1/orders - 멱등성 키 헤더를 커맨드로 전달한다")
	void createOrderPassesIdempotencyKeyToCommand() throws Exception {
		OrderDto.V1.OrderRequest request = new OrderDto.V1.OrderRequest(
			List.of(new OrderDto.V1.OrderItemRequest(1L, 1L)),
			null,
			CardType.KB,
			"1111-2222-3333-4444",
			"https://callback.test/orders"
		);

		when(orderApplicationService.createOrder(any()))
			.thenReturn(new OrderResult("ORD-1", USER_ID, 5000L, OrderStatus.PENDING));

		mockMvc.perform(post("/api/v1/orders")
				.header("X-USER-ID", USER_ID)
				.header("X-IDEMPOTENCY-KEY", "idem-key-1")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk());

		ArgumentCaptor<OrderCommand.CreateOrder> commandCaptor = ArgumentCaptor.forClass(OrderCommand.CreateOrder.class);
		verify(orderApplicationService, times(1)).createOrder(commandCaptor.capture());
		assertThat(commandCaptor.getValue().idempotencyKey()).isEqualTo("idem-key-1");
	}

	@Test
	@DisplayName("GET /api/v1/orders - 주문 목록 응답을 반환한다")
	void getOrdersReturnsOrderList() throws Exception {
		when(orderApplicationService.getOrders(any()))
			.thenReturn(List.of(
				new OrderResult("ORD-1", USER_ID, 10000L, OrderStatus.PENDING),
				new OrderResult("ORD-2", USER_ID, 5000L, OrderStatus.CONFIRMED)
			));

		mockMvc.perform(get("/api/v1/orders")
				.header("X-USER-ID", USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.meta.result").value("SUCCESS"))
			.andExpect(jsonPath("$.data[0].orderId").value("ORD-1"))
			.andExpect(jsonPath("$.data[1].orderId").value("ORD-2"));

		verify(orderApplicationService, times(1)).getOrders(any());
	}

	@Test
	@DisplayName("GET /api/v1/orders/{orderId} - 주문 상세 응답을 반환한다")
	void getOrderReturnsOrderDetail() throws Exception {
		when(orderApplicationService.getOrder(any()))
			.thenReturn(new OrderResult("ORD-123", USER_ID, 15000L, OrderStatus.PENDING));

		mockMvc.perform(get("/api/v1/orders/{orderId}", "ORD-123")
				.header("X-USER-ID", USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.meta.result").value("SUCCESS"))
			.andExpect(jsonPath("$.data.orderId").value("ORD-123"))
			.andExpect(jsonPath("$.data.userId").value(USER_ID))
			.andExpect(jsonPath("$.data.totalPrice").value(15000L))
			.andExpect(jsonPath("$.data.status").value("PENDING"));

		verify(orderApplicationService, times(1)).getOrder(any());
	}
}
