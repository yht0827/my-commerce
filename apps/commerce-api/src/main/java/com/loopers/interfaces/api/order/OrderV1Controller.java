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
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderQuery;
import com.loopers.application.order.OrderResult;
import com.loopers.interfaces.api.common.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderV1Controller {

	private final OrderFacade orderFacade;

	@PostMapping
	public ApiResponse<OrderDto.V1.OrderResponse> createOrder(@RequestHeader(value = "X-USER-ID") final String userId,
		@RequestBody final OrderDto.V1.OrderRequest orderRequest) {
		OrderCommand.CreateOrder command = orderRequest.toCommand(userId);
		OrderResult orderResult = orderFacade.createOrder(command);
		OrderDto.V1.OrderResponse orderResponse = OrderDto.V1.OrderResponse.from(orderResult);

		return ApiResponse.success(orderResponse);
	}

	@GetMapping
	public ApiResponse<List<OrderDto.V1.OrderResponse>> getOrders(@RequestHeader(value = "X-USER-ID") final String userId) {
		OrderQuery.GetOrders command = OrderDto.V1.getOrdersRequest.toCommand(userId);
		List<OrderResult> orderResults = orderFacade.getOrders(command);
		List<OrderDto.V1.OrderResponse> responses = orderResults.stream()
			.map(OrderDto.V1.OrderResponse::from)
			.toList();

		return ApiResponse.success(responses);
	}

	@GetMapping("/{orderId}")
	public ApiResponse<OrderDto.V1.OrderResponse> getOrder(@RequestHeader(value = "X-USER-ID") final String userId,
		@PathVariable Long orderId) {
		OrderQuery.GetOrder command = OrderDto.V1.getOrderRequest.toCommand(userId, orderId);
		OrderResult orderResult = orderFacade.getOrder(command);
		OrderDto.V1.OrderResponse orderResponse = OrderDto.V1.OrderResponse.from(orderResult);
		return ApiResponse.success(orderResponse);
	}

}
