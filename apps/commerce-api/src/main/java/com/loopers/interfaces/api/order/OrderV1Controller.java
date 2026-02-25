package com.loopers.interfaces.api.order;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loopers.application.order.OrderCommand;
import com.loopers.application.order.OrderApplicationService;
import com.loopers.application.order.OrderQuery;
import com.loopers.application.order.OrderResult;
import com.loopers.interfaces.api.common.ApiResponse;
import com.loopers.interfaces.api.common.CurrentUserId;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderV1Controller implements OrderV1ApiSpec {

	private final OrderApplicationService orderApplicationService;

	@PostMapping
	@Override
	public ApiResponse<OrderDto.V1.OrderResponse> createOrder(@CurrentUserId final String userId,
		@RequestHeader(value = "X-IDEMPOTENCY-KEY", required = false) final String idempotencyKey,
		@RequestBody final OrderDto.V1.OrderRequest orderRequest) {
		OrderCommand.CreateOrder command = orderRequest.toCommand(userId, idempotencyKey);
		OrderResult orderResult = orderApplicationService.createOrder(command);
		OrderDto.V1.OrderResponse orderResponse = OrderDto.V1.OrderResponse.from(orderResult);

		return ApiResponse.success(orderResponse);
	}

	@GetMapping
	@Override
	public ApiResponse<List<OrderDto.V1.OrderResponse>> getOrders(@CurrentUserId final String userId) {
		OrderQuery.GetOrders query = OrderDto.V1.GetOrdersRequest.toQuery(userId);
		List<OrderResult> orderResults = orderApplicationService.getOrders(query);
		List<OrderDto.V1.OrderResponse> responses = orderResults.stream()
			.map(OrderDto.V1.OrderResponse::from)
			.toList();

		return ApiResponse.success(responses);
	}

	@GetMapping("/{orderId}")
	@Override
	public ApiResponse<OrderDto.V1.OrderResponse> getOrder(@CurrentUserId final String userId,
		@PathVariable final String orderId) {
		OrderQuery.GetOrder query = OrderDto.V1.GetOrderRequest.toQuery(userId, orderId);
		OrderResult orderResult = orderApplicationService.getOrder(query);
		OrderDto.V1.OrderResponse orderResponse = OrderDto.V1.OrderResponse.from(orderResult);
		return ApiResponse.success(orderResponse);
	}

}
